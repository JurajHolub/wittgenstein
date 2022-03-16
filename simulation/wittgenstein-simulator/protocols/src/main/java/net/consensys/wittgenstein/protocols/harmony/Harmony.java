package net.consensys.wittgenstein.protocols.harmony;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.consensys.wittgenstein.core.*;
import net.consensys.wittgenstein.protocols.harmony.FBFT.Fbft;
import net.consensys.wittgenstein.protocols.harmony.output.CsvDumper;
import net.consensys.wittgenstein.protocols.harmony.output.OutputInfo;
import net.consensys.wittgenstein.protocols.harmony.rbs.RandomnessBasedSharding;
import net.consensys.wittgenstein.protocols.solana.Solana;
import org.apache.commons.cli.*;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Harmony implements Protocol {

    private final Network<HarmonyNode> network = new Network<>();
    private final HarmonyConfig harmonyConfig;
    private final NodeBuilder nb;
    public final StakeDistribution stakeDistribution;
    public final CsvDumper csvDumper = new CsvDumper();
    public final Map<Integer, List<Integer>> mapSlotToLeaders = new HashMap<>();

    private static final Logger logger;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Solana.class.getName());
    }

    public Harmony(HarmonyConfig harmonyConfig) {
        this.harmonyConfig = harmonyConfig;
        this.nb = new NodeBuilder.NodeBuilderWithRandomPosition();
        this.network.networkLatency = new NetworkLatency.NetworkFixedLatency(50);
        this.stakeDistribution = new StakeDistribution(network.rd, harmonyConfig);
    }

    @Override
    public Network<HarmonyNode> network() {
        return network;
    }

    @Override
    public Protocol copy() {
        return null;
    }

    @Override
    public void init() {
        stakeDistribution.redistributeToShards(0, 0);
        for (int node = 0; node < harmonyConfig.networkSize; node++) {
            boolean byzantine = (node < harmonyConfig.byzantineNodes);
            network.addNode(
                new HarmonyNode(
                    network.rd,
                    nb,
                    network,
                    stakeDistribution,
                    harmonyConfig,
                    logger,
                    csvDumper,
                    harmonyConfig.expectedTxPerBlock,
                    byzantine
                )
            );
        }
    }

    /** HarmonyNode */
    abstract static class AHarmonyNode extends Node {
        protected final transient Network<HarmonyNode> network;
        protected final transient StakeDistribution stakeDistribution;
        protected final transient HarmonyConfig harmonyConfig;
        protected final transient Logger logger;
        public final transient CsvDumper csvDumper;
        public final Fbft fbft;
        public final RandomnessBasedSharding rbs;
        public final int expectedTxPerBlock;

        public AHarmonyNode(
                Random rd,
                NodeBuilder nb,
                Network<HarmonyNode> network,
                StakeDistribution stakeDistribution,
                HarmonyConfig harmonyConfig,
                Logger logger,
                CsvDumper csvDumper,
                int expectedTps,
                boolean byzantine
        ) {
            super(rd, nb, byzantine);
            this.network = network;
            this.stakeDistribution = stakeDistribution;
            this.harmonyConfig = harmonyConfig;
            this.logger = logger;
            this.csvDumper = csvDumper;
            this.fbft = new Fbft(network.rd, nb, network, (HarmonyNode) this, stakeDistribution, logger, harmonyConfig);
            this.rbs = new RandomnessBasedSharding(rd, nb, network, (HarmonyNode) this, stakeDistribution);
            this.expectedTxPerBlock = expectedTps;
        }
    }

    public void simulateEpoch(int epoch, int numberOfEpochs) {
        int intervalForLoggingInSlots = 50;
        stakeDistribution.updateStakeDistribution();

        if (epoch != 0) {
            int rand = stakeDistribution.randForNextEpoch.poll();
            logger.info("Redistribute to shards for epoch "+ (epoch+1));
            stakeDistribution.redistributeToShards(rand, epoch);
            network.allNodes.forEach(HarmonyNode::cleanStats);
        }
        csvDumper.dumpEpochStake(epoch, stakeDistribution, network);
        for (int slot = 0; slot < harmonyConfig.epochDurationInSlots; slot++) {

            network.run(10);
            if (slot % intervalForLoggingInSlots == 0) {
                logger.info(String.format("Simulate epoch %d/%d, Slot %d/%d [%.2f%%]",
                        epoch+1, numberOfEpochs, slot, harmonyConfig.epochDurationInSlots, (double)slot / harmonyConfig.epochDurationInSlots * 100));
                csvDumper.dumpEpoch(epoch);
            }
            ddosAttack(slot);
            if (slot == harmonyConfig.epochDurationInSlots - 2*harmonyConfig.vdfInSlots) {
                HarmonyNode beaconChainLeader = network.allNodes.get(stakeDistribution.getBeaconShard().epochLeader);
                logger.info(String.format("Start DRS, node %d, epoch %d, slot %d, shard 0 (beacon)", beaconChainLeader.nodeId, epoch, slot));
                beaconChainLeader.onDistributedRandomnessGeneration(epoch, slot-1);
            }
            for (Shard shard : stakeDistribution.shards) {
                network.allNodes.get(shard.epochLeader).onSlot(epoch, slot, shard.shardId);
            }
        }
        //network.allNodes.forEach(node -> csvDumper.dumpEpoch(node, epoch, node.fbft.epochCommit));
    }
    public void simulate(int epochs) {

        for (int epoch = 0; epoch < epochs; epoch++) {
            simulateEpoch(epoch, epochs);
            network.run(10);
            csvDumper.dumpEpoch(epoch);
        }
        csvDumper.dumpLeaders(stakeDistribution.leaders);
    }

    public void prepareDdosAttack() {
        if (!harmonyConfig.ddosAttacks) return;

        int numberOfShards = stakeDistribution.shards.size();

        int step = harmonyConfig.epochDurationInSlots / (numberOfShards*2);
        int numberLeadersUnderAttack = 1;

        for (int slot = step; slot < harmonyConfig.epochDurationInSlots; slot += step) {
            if (numberLeadersUnderAttack > numberOfShards) break;


            List<Integer> ddosedLeaders = IntStream.range(0, numberLeadersUnderAttack)
                    .map(shard -> stakeDistribution.shards.get(shard).epochLeader)
                    .boxed()
                    .collect(Collectors.toList());
            mapSlotToLeaders.put(slot, ddosedLeaders);

            numberLeadersUnderAttack++;
        }
        logger.info("DDoS attack: " + mapSlotToLeaders);
    }

    public void ddosAttack(int slot) {
        List<Integer> startDdos = mapSlotToLeaders.get(slot);
        if (startDdos != null) {
            for (Integer ddosLeader : startDdos) {
                network.allNodes.get(ddosLeader).stop();
            }
        }

        int numberOfShards = stakeDistribution.shards.size();
        int step = harmonyConfig.epochDurationInSlots / (numberOfShards*2);
        if (slot == harmonyConfig.epochDurationInSlots - step) {
            network.allNodes.forEach(Node::start);
        }
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options()
            .addOption(
                Option.builder().longOpt("config")
                    .required(true)
                    .hasArg().type(String.class)
                    .desc("Path to config file with input parameters for given protocol.")
                    .build()
            );
        CommandLine cmd;
        try {
            cmd = (new DefaultParser()).parse(options, args);
        }
        catch (ParseException e) {
            new HelpFormatter().printHelp("Help", options);
            return;
        }
        String config = cmd.getParsedOptionValue("config").toString();

        ObjectMapper objectMapper = new ObjectMapper();
        HarmonyConfig harmonyConfig = objectMapper.readValue(new File(config), HarmonyConfig.class);
        Harmony harmony = new Harmony(harmonyConfig);
        harmony.init();
        harmony.prepareDdosAttack();
        logger.info("Start simulation of Harmony.");

        logger.info("Clean output directory.");
        harmony.csvDumper.cleanData();

        logger.info("Input parameters:");
        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(harmonyConfig));
        harmony.simulate(harmonyConfig.numberOfEpochs);
        logger.info("End simulation of Harmony.");
    }

    public static OutputInfo run(HarmonyConfig harmonyConfig) throws IOException {
        Harmony harmony = new Harmony(harmonyConfig);
        harmony.init();
        harmony.prepareDdosAttack();

        logger.info("Start simulation of Harmony.");

        logger.info("Clean output directory.");
        harmony.csvDumper.cleanData();

        logger.info("Input parameters:");
        logger.info((new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(harmonyConfig));
        harmony.simulate(harmonyConfig.numberOfEpochs);
        logger.info("End simulation of Harmony.");
        return harmony.csvDumper.outputInfo();
    }

}
