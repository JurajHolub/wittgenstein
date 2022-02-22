package net.consensys.wittgenstein.protocols.solana;

import net.consensys.wittgenstein.core.*;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import static net.consensys.wittgenstein.protocols.solana.SolanaConfig.*;
import net.consensys.wittgenstein.core.messages.Message;
import org.apache.commons.cli.*;

public class Solana implements Protocol {

    private static final Logger logger;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Solana.class.getName());
    }

    private final NodeStateDump stats;
    private final Network<SolNode> network = new Network<>();
    private final int networkSize;
    private final NodeBuilder nb;
    private final EpochDataContainer<StakeDistribution> stakeDistribution;
    private final EpochDataContainer<List<Integer>> leaderSchedule;
    public int validatorReliability = 100;
    public int expectedTps = 3000;

    public Solana(int networkSize, int expectedTps) {

        this.networkSize = networkSize;
        this.expectedTps = expectedTps;
        this.stats = new NodeStateDump(networkSize);
        this.nb = new NodeBuilder.NodeBuilderWithRandomPosition();
        this.stakeDistribution = new EpochDataContainer<>(
            new StakeDistribution(networkSize),
            new StakeDistribution(networkSize)
        );
        this.leaderSchedule = new EpochDataContainer<>(
            new ArrayList<>(SolanaConfig.EPOCH_DURATION_IN_SLOTS),
            new ArrayList<>(SolanaConfig.EPOCH_DURATION_IN_SLOTS)
        );
    }

    public EpochDataContainer<StakeDistribution> getStakeDistribution() {
        return stakeDistribution;
    }

    public void setNetworkLatency(NetworkLatency networkLatency) {
        this.network.setNetworkLatency(networkLatency);
    }

    @Override
    public Network<SolNode> network() {
        return network;
    }

    @Override
    public Protocol copy() {
        return null;
    }

    @Override
    public void init() {
        IntStream.range(0, networkSize).forEach(i -> {
            SolNode node = new SolNode(network.rd, nb, false);
            network.addNode(node);
        });
        calculateLeaderSchedule(0, false);
    }

    public void calculateLeaderSchedule(int epoch, boolean next) {
        StakeDistribution stake = stakeDistribution.getCurrent();
        List<Integer> schedule = next ? leaderSchedule.getNext() : leaderSchedule.getCurrent();
        schedule.clear();

        AliasMethod aliasMethod =
                new AliasMethod(stake.getNodesProbability(), new Random(epoch+42)); // +42 to prevent 0 seed at epoch 0.

        IntStream.range(0, SolanaConfig.EPOCH_DURATION_IN_SLOTS)
                .forEach(i -> schedule.add(aliasMethod.next()));

    }

    /** Simulate redistribution of stake after each epoch. Real stake distribution would depend on transactions
     * included into current epoch, but we do not simulate data layer. As a result, we cannot change stake distribution
     * from real data in transaction.
     */
    public void updateStakeDistribution() {
        stakeDistribution.swap();
        leaderSchedule.swap();

        int thousandthOfTotalStake = stakeDistribution.getPrevious().getTotalStake() / 1000;
        int totalChange = network.rd.nextInt(thousandthOfTotalStake);

        for (SolNode node : network.allNodes) {
            int oldStake = stakeDistribution.getPrevious().getNodesStake().get(node.nodeId);
            int change = network.rd.nextInt(totalChange);
            totalChange -= change;
            int newStake = oldStake + change;
            stakeDistribution.getCurrent().setStake(node.nodeId, newStake);
        }
    }

    public EpochDataContainer<List<Integer>> getLeaderSchedule() {
        return leaderSchedule;
    }

    public SolNode getSlotLeader(int slot) {
        int id = leaderSchedule.getCurrent().get(slot);
        return network.allNodes.get(id);
    }

    /** Messages */
    public class VoteMessage extends Message<SolNode> {
        final int slot;
        final int epoch;

        public VoteMessage(int epoch, int slot) {
            this.slot = slot;
            this.epoch = epoch;
        }

        @Override
        public void action(Network<SolNode> network, SolNode from, SolNode to) {
            to.onVoteReceive(from, epoch, slot);
        }
    }
    public class SlotMessage extends Message<SolNode> {
        final int slot;
        final SlotData data;
        final int epoch;

        public SlotMessage(int epoch, int slot, SlotData data) {
            this.slot = slot;
            this.epoch = epoch;
            this.data = data;
        }

        @Override
        public void action(Network<SolNode> network, SolNode from, SolNode to) {
            to.onSlotReceive(from, epoch, slot, data);
        }
    }
    /** Messages */

    /** SolNode */
    public class SolNode extends Node {
        int slot = -1;
        int epoch = 0;
        public final EpochDataContainer<Map<Integer, SlotData>> slotMetaDatas;
        public final SlotData txStore;
        public boolean underDDoS = false;

        public SolNode(Random rd, NodeBuilder nb, boolean byzantine) {
            super(rd, nb, byzantine);
            slotMetaDatas = new EpochDataContainer<>(
                new HashMap<>(),
                new HashMap<>()
            );
            txStore = new SlotData();
        }

        public void onVoteReceive(SolNode from, int voteForEpoch, int voteForSlot) {
            if (underDDoS) return;

            if (voteForEpoch == epoch) {
                SlotData md = slotMetaDatas.getCurrent().getOrDefault(voteForSlot, new SlotData());
                md.txCounterVote++;
                md.receivedVotingPower += stakeDistribution.getCurrent().getStake(from.nodeId).nodeStake;
                md.totalVotingPower = stakeDistribution.getCurrent().getTotalStake();
                slotMetaDatas.getCurrent().put(voteForSlot, md);
            }
            else {
                SlotData md = slotMetaDatas.getPrevious().getOrDefault(voteForSlot, new SlotData());
                md.txCounterVote++;
                md.receivedVotingPower += stakeDistribution.getPrevious().getStake(from.nodeId).nodeStake;
                md.totalVotingPower = stakeDistribution.getPrevious().getTotalStake();
                slotMetaDatas.getPrevious().put(voteForSlot, md);
            }
            txStore.txCounterVote++;
            txStore.receivedVotingPower += (voteForEpoch == epoch)
                    ? stakeDistribution.getCurrent().getStake(from.nodeId).nodeStake
                    : stakeDistribution.getPrevious().getStake(from.nodeId).nodeStake;
            txStore.totalVotingPower = (voteForEpoch == epoch)
                    ? stakeDistribution.getCurrent().getStake(from.nodeId).nodeStake
                    : stakeDistribution.getPrevious().getStake(from.nodeId).nodeStake;
        }

        public void onSlotReceive(SolNode from, int epoch, int slot, SlotData data) {
            data.arriveTime = network.time;

            if (epoch == this.epoch) {
                slotMetaDatas.getCurrent().put(slot, data);
            }
            else {
                slotMetaDatas.getPrevious().put(slot, data);
            }

            if (slotMetaDatas.getCurrent().size() == EPOCH_DURATION_IN_SLOTS) {
                this.slotMetaDatas.swap();
                this.slotMetaDatas.getCurrent().clear();
                stats.dumpSlotState(this, epoch, this.slotMetaDatas.getPrevious());
                this.epoch++;
            }

            this.slot = slot;

            if (network.rd.nextInt(100) > validatorReliability) {
                return; // node do not vote for some reason (network or node problems)
            }

            int nextSlotLeader = slot+1 == EPOCH_DURATION_IN_SLOTS ? leaderSchedule.getNext().get(0) : leaderSchedule.getCurrent().get(slot+1);
            network.send(new VoteMessage(epoch, slot), this, network.allNodes.get(nextSlotLeader));
        }

        public void onSlotEnd(int epoch, int slot) {
            if (!isLeader(slot)) return;


            if (underDDoS) {
                txStore.reset();
                slotMetaDatas.getCurrent().put(slot, txStore);
                network.send(new SlotMessage(epoch, slot, txStore), this, network.allNodes);
            }
            else {
                int stdDeviation = expectedTps / 10;
                int mean = expectedTps;
                int lambda = (int) network.rd.nextGaussian() * stdDeviation + mean;
                txStore.txCounterNonVote = tpsToTpb(expectedTps) + lambda;
                //(int) network.rd.nextGaussian()*TxPerSlot.STD_DEVIATION + NonVotesPerSlot.MEAN;
                slotMetaDatas.getCurrent().put(slot, txStore);
                network.send(new SlotMessage(epoch, slot, txStore), this, network.allNodes);
            }

            txStore.reset();
        }

        /** Transform TPS into Transaction Per Block. */
        public int tpsToTpb(int tps) {
            return (int) (tps / 1000.0 * SLOT_DURATION_IN_MS);
        }

        public boolean isLeader(int slot) {
            return leaderSchedule.getCurrent().get(slot) == nodeId;
        }

    } /** SolNode */

    public void simulateSlot(int epoch, int slot) {
        int slotLeader = leaderSchedule.getCurrent().get(slot);
        SolNode leader = network.allNodes.get(slotLeader);
        leader.onSlotEnd(epoch, slot);
    }

    public void simulateEpoch(int epoch, int numberOfEpochs) {
        int intervalForLoggingInSlots = 100;
        for (int slot = 0; slot < EPOCH_DURATION_IN_SLOTS; slot++) {
            if (slot % intervalForLoggingInSlots == 0) {
                logger.info(String.format("Simulate epoch %d/%d, Slot %d/%d [%.2f%%]",
                        epoch, numberOfEpochs, slot, EPOCH_DURATION_IN_SLOTS, (double)slot / EPOCH_DURATION_IN_SLOTS * 100));
            }

            int stdDeviation = 20;
            int mean = 100;
            int delay = 180 - (int)(network.rd.nextGaussian()*stdDeviation + mean);
            network.runMs(SLOT_DURATION_IN_MS + delay);
            simulateSlot(epoch, slot);
            if (slot == LEADER_SCHEDULE_TRIGGER) {
                calculateLeaderSchedule(epoch + 1, true);
            }
        }
    }

    public void simulate(int numberOfEpochs) {
        int maxNumberOfSlots = Integer.MAX_VALUE / SLOT_DURATION_IN_MS;
        int maxNumberOfEpochs = maxNumberOfSlots / EPOCH_DURATION_IN_SLOTS;

        if (numberOfEpochs > maxNumberOfEpochs) throw new IllegalArgumentException("Maximum allowed simulation time is " + maxNumberOfEpochs + " epochs.");

        for (int epoch = 0; epoch < numberOfEpochs; epoch++) {

            logger.info(String.format("Start epoch %d/%d", epoch, numberOfEpochs));
            stats.dumpStakeState(epoch, stakeDistribution.getCurrent(), leaderSchedule.getCurrent(), network.allNodes);
            simulateEpoch(epoch, numberOfEpochs);

            updateStakeDistribution();
        }
        network().run(5000); // enough time to receive last messages
    }

    public static void main(String[] args) throws ParseException {

        Option networkSizeOption = Option.builder("n").longOpt("nodes")
                .required(true)
                .hasArg().type(Number.class)
                .desc("Network size (number of nodes).")
                .build();
        Option epochSizeOption = Option.builder("e").longOpt("epochs")
                .required(true)
                .hasArg().type(Number.class)
                .desc("Number of epochs to simulate.")
                .build();
        Option tpsOption = Option.builder().longOpt("tps")
                .required(true)
                .hasArg().type(Number.class)
                .desc("TPS (Transaction per second) value set up network nodes to generated enough transaction for given TPS.")
                .build();
        Option voteRatioOption = Option.builder().longOpt("validator-reliability")
                .hasArg().type(Number.class)
                .desc("Integer in range <0, 100> that determines validator reliability in term of voting.")
                .build();
        Option underDdosOption = Option.builder().longOpt("ddos-attack")
                .required(false)
                .hasArg().type(Number.class)
                .desc("Simulate DDoS attack to given number of nodes (the most rich one).")
                .build();
        Options options = new Options()
                .addOption(networkSizeOption)
                .addOption(epochSizeOption)
                .addOption(tpsOption)
                .addOption(voteRatioOption)
                .addOption(underDdosOption);

        CommandLine cmd;
        try {
            cmd = (new DefaultParser()).parse(options, args);
        }
        catch (ParseException e) {
            new HelpFormatter().printHelp("Help", options);
            return;
        }

        int networkSize = ((Number) cmd.getParsedOptionValue("nodes")).intValue();
        int numberOfEpochs = ((Number) cmd.getParsedOptionValue("epochs")).intValue();
        int expectedTps = ((Number) cmd.getParsedOptionValue("tps")).intValue();

        logger.info("Start simulation of Solana.");
        Solana sol = new Solana(networkSize, expectedTps);
        sol.init();

        if (cmd.hasOption("validator-reliability")) {
            sol.validatorReliability = ((Number) cmd.getParsedOptionValue("validator-reliability")).intValue();
            logger.info("Validator will vote with reliability "+sol.validatorReliability+"%.");
        }

        if (cmd.hasOption("ddos-attack")) {
            int numberOfNodesUnderAttack = ((Number) cmd.getParsedOptionValue("ddos-attack")).intValue();
            logger.info("Simulate DDoS attack to the "+numberOfNodesUnderAttack+"th richest nodes.");
            for (int i = 0; i < numberOfNodesUnderAttack; i++) {
                sol.network.allNodes.get(i).underDDoS = true;
            }
        }

        logger.info("Clean output directory.");
        sol.stats.cleanData();
        logger.info(String.format("Network size in nodes: %d.", networkSize));
        logger.info(String.format("Number of epoch to simulate: %d.", numberOfEpochs));
        sol.simulate(numberOfEpochs);
        logger.info("End simulation of Solana.");
    }
}

