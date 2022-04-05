package net.consensys.wittgenstein.protocols.solana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.consensys.wittgenstein.core.*;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import net.consensys.wittgenstein.protocols.solana.TowerBFT.TowerBFT;
import net.consensys.wittgenstein.protocols.solana.output.NodeSlot;
import net.consensys.wittgenstein.protocols.solana.output.OutputDumper;
import net.consensys.wittgenstein.protocols.utils.AliasMethod;
import net.consensys.wittgenstein.protocols.utils.DosAttackUtil;
import org.apache.commons.cli.*;

/**
 * Simulation of Solana consensus.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class Solana implements Protocol {

    private static final Logger logger;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Solana.class.getName());
    }

    private final SolanaConfig solanaConfig;
    private final OutputDumper stats;
    private final Network<SolanaNode> network = new Network<>();
    private final NodeBuilder nb;
    private final StakeDistribution stakeDistribution;
    private final List<Integer> leaderSchedule;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DosAttackUtil dosAttackUtil = new DosAttackUtil();

    public Solana(SolanaConfig solanaConfig) throws UnknownHostException {
        this.solanaConfig = solanaConfig;
        this.stats = new OutputDumper(solanaConfig);
        this.nb = new NodeBuilder.NodeBuilderWithRandomPosition();
        this.stakeDistribution = new StakeDistribution(solanaConfig, network.rd);
        this.leaderSchedule = new ArrayList<>(solanaConfig.epochDurationInSlots);
    }

    public StakeDistribution getStakeDistribution() {
        return stakeDistribution;
    }

    public void setNetworkLatency(NetworkLatency networkLatency) {
        this.network.setNetworkLatency(networkLatency);
    }

    @Override
    public Network<SolanaNode> network() {
        return network;
    }

    @Override
    public Protocol copy() {
        return null;
    }

    @Override
    public void init() {
        IntStream.range(0, solanaConfig.networkSize).forEach(i -> {
            SolanaNode node = new SolanaNode(
                network.rd,
                nb,
                network,
                stakeDistribution,
                solanaConfig,
                logger,
                false,
                leaderSchedule,
                stats
            );
            network.addNode(node);
        });

        setLeadersForEpoch(0);
    }

    public void calculateLeaderSchedule(int epoch) {
        StakeDistribution stake = stakeDistribution;
        leaderSchedule.clear();

        AliasMethod aliasMethod =
                new AliasMethod(stake.getNodesProbability(), new Random(epoch+42)); // +42 to prevent 0 seed at epoch 0.

        IntStream.range(0, solanaConfig.epochDurationInSlots)
                .forEach(i -> leaderSchedule.add(aliasMethod.next()));
    }

    public void setLeadersForEpoch(int epoch) {
        if (solanaConfig.vrfLeaderSelection) {
            this.stakeDistribution.updateVRF(epoch);
        }
        else {
            calculateLeaderSchedule(epoch);
        }
    }

    /** Simulate redistribution of stake after each epoch. Real stake distribution would depend on transactions
     * included into current epoch, but we do not simulate data layer. As a result, we cannot change stake distribution
     * from real data in transaction.
     */
    public void updateStakeDistribution(int epoch) {
        stakeDistribution.setNodesStake(stakeDistribution.stakeDistributionUtil.updateStakeDistribution(
            epoch,
            stakeDistribution.getNodesStake()
        ));
        stakeDistribution.setNodesProbability(stakeDistribution.stakeDistributionUtil.updateStakeProbability(
            epoch,
            stakeDistribution.getNodesStake(),
            stakeDistribution.getNodesProbability()
        ));

        for (int node = 0; node < stakeDistribution.getNodesStake().size(); node++) {
            stakeDistribution.vrfLeaderSelection.updateStake(epoch, stakeDistribution.getNodesProbability());
        }
    }

    public List<Integer> getLeaderSchedule() {
        return leaderSchedule;
    }

    public SolanaNode getSlotLeader(int slot) {
        int id = leaderSchedule.get(slot);
        return network.allNodes.get(id);
    }

    /** SolanaNode */
    abstract static class ASolanaNode extends Node {
        protected final transient Network<SolanaNode> network;
        protected final transient SolanaConfig solanaConfig;
        protected final transient Logger logger;
        public final transient OutputDumper stats;
        protected final StakeDistribution stakeDistribution;
        protected final List<Integer> leaderSchedule;
        public TowerBFT towerBFT;

        public ASolanaNode(
                Random rd,
                NodeBuilder nb,
                Network<SolanaNode> network,
                StakeDistribution stakeDistribution,
                SolanaConfig solanaConfig,
                Logger logger,
                boolean byzantine,
                List<Integer> leaderSchedule,
                OutputDumper stats
        ) {
            super(rd, nb, byzantine);
            this.network = network;
            this.stakeDistribution = stakeDistribution;
            this.solanaConfig = solanaConfig;
            this.logger = logger;
            this.leaderSchedule = leaderSchedule;
            this.stats = stats;
            this.towerBFT = new TowerBFT((SolanaNode) this, stakeDistribution, leaderSchedule, network, stats);
        }

    }

    public void simulateSlot(int epoch, int slot) {
        int slotLeader = (solanaConfig.vrfLeaderSelection)
                ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(slot)
                : leaderSchedule.get(slot);

        SolanaNode leader = network.allNodes.get(slotLeader);
        leader.onSlotMessage(epoch, slot);
        network.runMs(solanaConfig.slotDurationInMs);

        if (leader.underDDoS) {
            network.allNodes.forEach(node -> node.stats.dumpSlotState(
                new NodeSlot(
                    node.nodeId,
                    new Block(slot, epoch),
                    network.time,
                    0,
                    0,
                    0,
                    0
                )
            ));
        }
        else {
            leader.onSlotEnd(slot, epoch);
        }
    }

    public void simulateEpoch(int epoch, int numberOfEpochs) {
        int intervalForLoggingInSlots = 100;
        for (int slot = 0; slot < solanaConfig.epochDurationInSlots; slot++) {
            if (slot % intervalForLoggingInSlots == 0) {
                logger.info(String.format("Simulate epoch %d/%d, Slot %d/%d [%.2f%%]",
                        epoch, numberOfEpochs, slot, solanaConfig.epochDurationInSlots, (double)slot / solanaConfig.epochDurationInSlots * 100));
            }
            simulateSlot(epoch, slot);
            if (slot == solanaConfig.leaderScheduleTrigger) {
                setLeadersForEpoch(epoch+1);
            }
        }
    }

    public void simulate(int numberOfEpochs) {
        int maxNumberOfSlots = Integer.MAX_VALUE / solanaConfig.slotDurationInMs;
        int maxNumberOfEpochs = maxNumberOfSlots / solanaConfig.epochDurationInSlots;

        if (numberOfEpochs > maxNumberOfEpochs) throw new IllegalArgumentException("Maximum allowed simulation time is " + maxNumberOfEpochs + " epochs.");

        network.run(1);
        for (int epoch = 0; epoch < numberOfEpochs; epoch++) {

            logger.info(String.format("Start epoch %d/%d", epoch, numberOfEpochs));
            stats.dumpStakeState(epoch, stakeDistribution, leaderSchedule, network.allNodes);
            simulateEpoch(epoch, numberOfEpochs);

            updateStakeDistribution(epoch);
        }
        network().run(5000); // enough time to receive last messages
    }

    public void prepareDdosAttack() throws JsonProcessingException {
        if (solanaConfig.numberOfNodesUnderAttack == 0) return;

        List<Integer> leaders = (solanaConfig.vrfLeaderSelection)
                ? dosAttackUtil.bestLeadersToAttackWithVDF(stakeDistribution.getNodesStake(), solanaConfig.numberOfNodesUnderAttack)
                : dosAttackUtil.bestLeadersToAttackWithSchedule(leaderSchedule, solanaConfig.numberOfNodesUnderAttack);

        for (int nodeId : leaders) {
            network.allNodes.get(nodeId).underDDoS = true;
        }

        logger.info("Nodes under ddos:");
        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(network.allNodes.stream().filter(node -> node.underDDoS)));
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

        SolanaConfig solanaConfig = (new ObjectMapper()).readValue(new File(config), SolanaConfig.class);
        Solana solana = new Solana(solanaConfig);
        solana.init();
        solana.prepareDdosAttack();
        logger.info("Start simulation of Solana.");
        logger.info(solana.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(solanaConfig));
        solana.simulate(solanaConfig.numberOfEpochs);
        logger.info("End simulation of Solana.");
    }

    public static void run(SolanaConfig solanaConfig) throws IOException {
        Solana solana = new Solana(solanaConfig);
        solana.init();
        solana.prepareDdosAttack();

        logger.info("Start simulation of Solana.");
        ObjectMapper objectMapper = new ObjectMapper();
        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(solanaConfig));
        solana.simulate(solanaConfig.numberOfEpochs);
        logger.info("End simulation of Solana.");
    }
}

