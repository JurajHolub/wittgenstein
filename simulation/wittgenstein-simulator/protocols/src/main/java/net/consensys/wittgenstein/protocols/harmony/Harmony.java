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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;


public class Harmony implements Protocol {

    private final Network<HarmonyNode> network = new Network<>();
    private final HarmonyConfig harmonyConfig;
    private final NodeBuilder nb;
    public final StakeDistribution stakeDistribution;
    public final CsvDumper csvDumper = new CsvDumper();

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

            network.run(5);
            if (slot % intervalForLoggingInSlots == 0) {
                logger.info(String.format("Simulate epoch %d/%d, Slot %d/%d [%.2f%%]",
                        epoch+1, numberOfEpochs, slot, harmonyConfig.epochDurationInSlots, (double)slot / harmonyConfig.epochDurationInSlots * 100));
                csvDumper.dumpEpoch(epoch);
            }
            if (slot == harmonyConfig.epochDurationInSlots - 2*harmonyConfig.vdfInSlots) {
                HarmonyNode beaconChainLeader = network.allNodes.get(stakeDistribution.getBeaconShard().epochLeader);
                logger.info(String.format("Start DRS, node %d, epoch %d, slot %d, shard 0 (beacon)", beaconChainLeader.nodeId, epoch, slot));
                beaconChainLeader.onDistributedRandomnessGeneration(epoch, slot-1);
            }
            for (Shard shard : stakeDistribution.shards) {
                network.allNodes.get(shard.epochLeader).onSlot(epoch, slot, shard.shardId);
            }
        }
        csvDumper.dumpEpoch(epoch);
        //network.allNodes.forEach(node -> csvDumper.dumpEpoch(node, epoch, node.fbft.epochCommit));
    }
    public void simulate(int epochs) {

        for (int epoch = 0; epoch < epochs; epoch++) {
            simulateEpoch(epoch, epochs);
        }
        network.run(10);
        csvDumper.dumpLeaders(stakeDistribution.leaders);
    }

    public static void main(String[] args) throws ParseException, IOException {
//        Options options = new Options()
//            .addOption(
//                Option.builder().longOpt("tpb")
//                    .required(true)
//                    .hasArg().type(Number.class)
//                    .desc("TPB (Transaction per block) value set up network nodes to generated enough transaction for given TPB.")
//                    .build()
//            )
//            .addOption(
//                Option.builder().longOpt("slots")
//                    .required(true)
//                    .hasArg().type(Number.class)
//                    .desc("Number of slots in every epoch.")
//                    .build()
//            )
//            .addOption(
//                Option.builder("e").longOpt("epochs")
//                    .required(true)
//                    .hasArg().type(Number.class)
//                    .desc("Number of epochs to simulate.")
//                    .build()
//            )
//            .addOption(
//                Option.builder("n").longOpt("nodes")
//                    .required(true)
//                    .hasArg().type(Number.class)
//                    .desc("Network size (number of nodes).")
//                    .build()
//            )
//            .addOption(
//                Option.builder("s").longOpt("shards")
//                    .required(true)
//                    .hasArg().type(Number.class)
//                    .desc("Number of shards.")
//                    .build()
//            )
//            .addOption(
//                Option.builder().longOpt("lambda")
//                    .required(false)
//                    .hasArg().type(Number.class)
//                    .desc("Lambda for token size (default 600).")
//                    .build()
//            )
//            .addOption(
//                Option.builder().longOpt("epoch-duration")
//                    .required(false)
//                    .hasArg().type(Number.class)
//                    .desc("Epoch duration in slots.")
//                    .build()
//            )
//            .addOption(
//                Option.builder().longOpt("byzantine")
//                    .required(false)
//                    .hasArg().type(Number.class)
//                    .desc("Number of nodes, which are malicious.")
//                    .build()
//        );
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

//        int numberOfShards = ((Number) cmd.getParsedOptionValue("shards")).intValue();
//        int networkSize = ((Number) cmd.getParsedOptionValue("nodes")).intValue();
//        int numberOfSlots = ((Number) cmd.getParsedOptionValue("slots")).intValue();
//        int numberOfEpochs = ((Number) cmd.getParsedOptionValue("epochs")).intValue();
//        int expectedTxPerBlock = ((Number) cmd.getParsedOptionValue("tpb")).intValue();
//        int lambda = 600;
//        int byzantine = 0;
//        if (cmd.hasOption("lambda")) {
//            lambda = ((Number) cmd.getParsedOptionValue("lambda")).intValue();
//        }
//        if (cmd.hasOption("byzantine")) {
//            byzantine = ((Number) cmd.getParsedOptionValue("byzantine")).intValue();
//        }
//        if (cmd.hasOption("epoch-duration")) {
//            byzantine = ((Number) cmd.getParsedOptionValue("byzantine")).intValue();
//        }

        ObjectMapper objectMapper = new ObjectMapper();
        HarmonyConfig harmonyConfig = objectMapper.readValue(new File(config), HarmonyConfig.class);
        Harmony harmony = new Harmony(harmonyConfig);
        harmony.init();

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
