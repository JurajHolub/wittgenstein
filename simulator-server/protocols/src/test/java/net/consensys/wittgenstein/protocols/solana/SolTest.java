package net.consensys.wittgenstein.protocols.solana;

import junit.framework.TestCase;
import net.consensys.wittgenstein.core.NetworkLatency;
import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static net.consensys.wittgenstein.protocols.solana.SolanaConfig.*;

public class SolTest extends TestCase {

    private Solana sol;
    private int networkSize;

    @Override
    protected void setUp() throws Exception {
        networkSize = 1000;
        sol = new Solana(networkSize, 3000);
        sol.init();
    }

    public void testLeaderSchedule() {
        EPOCH_DURATION_IN_SLOTS = 400_000;
        sol.calculateLeaderSchedule(0, false);
        List<Integer> leaderSchedule = sol.getLeaderSchedule().getCurrent();

        Assert.assertEquals(SolanaConfig.EPOCH_DURATION_IN_SLOTS, leaderSchedule.size());
        Assert.assertEquals(networkSize - 1, Collections.max(leaderSchedule).intValue());
        Assert.assertEquals(0, Collections.min(leaderSchedule).intValue());

        Map<Integer, Long> histogram = leaderSchedule.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        histogram.forEach((leader, count) -> {
            Assert.assertEquals(sol.getStakeDistribution().getCurrent().getNodesProbability().get(leader) * SolanaConfig.EPOCH_DURATION_IN_SLOTS, (double)count, SolanaConfig.EPOCH_DURATION_IN_SLOTS * 0.01);
        });
    }

    public void testSlotLeaderGenesis() {
        sol.network().networkLatency = new NetworkLatency.NetworkFixedLatency(100);
        Solana.SolNode firstLeader = sol.network().allNodes.get(sol.getLeaderSchedule().getCurrent().get(0));

        // Leader=L, Validator=V
        // Time:       ------400------,------401------,------402------,  ... ,------501-------
        // Receive time:      |               |               |                      |
        // Leader:      L.bcastStart()   L.bcastEnd()  L.receiveBcast()         V.receiveBcas()
        sol.network().runMs(SLOT_DURATION_IN_MS);
        sol.simulateSlot(0, 0);
        sol.network().runMs(1000); // run more than 1 slot so all messages can arrive

        Assert.assertEquals(sol.network().allNodes.size(), sol.network().liveNodes().size());
        for (Solana.SolNode solNode : sol.network().liveNodes()) {
            SlotData md = solNode.slotMetaDatas.getCurrent().get(0);
            Assert.assertNotNull(md);
            if (solNode.nodeId == firstLeader.nodeId) {
                Assert.assertEquals(402, md.arriveTime);
            }
            else {
                Assert.assertEquals("Node <"+solNode.nodeId+">",501, md.arriveTime);
            }
        }
    }

//    public void testSlotLeadersForEpoch() {
//        sol.network().networkLatency = new NetworkLatency.NetworkFixedLatency(100);
//
//        sol.simulateEpoch(0);
//
//        Assert.assertEquals(sol.network().allNodes.size(), sol.network().liveNodes().size());
//
//        for (Sol.SolNode solNode : sol.network().liveNodes()) {
//            for (int slot = 1; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
//                SlotData md = solNode.slotMetaDatas.getCurrent().get(slot);
//
//                if (slot == SolanaConfig.EPOCH_DURATION_IN_SLOTS - 1) {
//                    Assert.assertNull("[Slot " + slot + ", Node " + solNode.nodeId + "]", md);
//                    continue;
//                }
//
//                Assert.assertNotNull("[Slot " + slot + ", Node " + solNode.nodeId + "]", md);
//                Sol.SolNode leader = sol.getSlotLeader(slot);
//                if (leader.nodeId != solNode.nodeId) {
//                    // Slot 0 expected 501, Slot 1 expected 901, Slot 2 expected 1301 ==> slot_len * (1+slot_num) + delay
//                    Assert.assertEquals("[Slot "+slot+", Node "+solNode.nodeId+",Leader "+leader.nodeId+"]",
//                            SolanaConfig.SLOT_DURATION_IN_MS * (1 + slot) + 101,
//                            md.arriveTime
//                    );
//                }
//                else {
//                    // delay of broadcast from node A -> A is 2ms
//                    Assert.assertEquals("[Slot "+slot+", Node "+solNode.nodeId+",Leader "+leader.nodeId+"]",
//                            SolanaConfig.SLOT_DURATION_IN_MS * (1 + slot) + 2,
//                            md.arriveTime
//                    );
//                }
//            }
//        }
//    }

    public void testSlotLeadersForMultipleEpochs() {
        sol.network().networkLatency = new NetworkLatency.NetworkFixedLatency(100);

        // copy initial stake
        List<Integer> nodesStake = new ArrayList<>(sol.getStakeDistribution().getCurrent().getNodesStake());
        int totalStake = sol.getStakeDistribution().getCurrent().getTotalStake();
        List<Double> nodesProbability = new ArrayList<>(sol.getStakeDistribution().getCurrent().getNodesProbability());
        StakeDistribution oldStakeDistribution = new StakeDistribution(totalStake, nodesStake, nodesProbability);

        sol.simulate(10);

        StakeDistribution newStakeDistribution = sol.getStakeDistribution().getCurrent();

        sol.network().allNodes.forEach(node -> {
            StakeDistribution.Stake old = oldStakeDistribution.getStake(node.nodeId);
            StakeDistribution.Stake neww = newStakeDistribution.getStake(node.nodeId);
            String oldDump = String.format("Stake %d/%d (%.4f%%)", old.nodeStake, old.totalStake, old.nodeProbability);
            String newDump = String.format("Stake %d/%d (%.4f%%)", neww.nodeStake, neww.totalStake, neww.nodeProbability);
            if (old.nodeProbability > 0.01) { // node with such a high stake must be active and manipulate his resources
                Assert.assertNotEquals(String.format("Node %d, %s | %s", node.nodeId, oldDump, newDump), old.nodeStake, neww.nodeStake);
            }
        });

        Assert.assertEquals(SLOT_DURATION_IN_MS * SolanaConfig.EPOCH_DURATION_IN_SLOTS * 10, sol.network().time);
    }

//    public void testVoting() {
//        sol.network().networkLatency = new NetworkLatency.NetworkFixedLatency(100);
//
//        for (int epoch = 0; epoch < 3; epoch++) {
//            sol.simulateEpoch(epoch);
//            sol.updateStakeDistribution();
//            sol.network().runMs(200);
//
//            for (Sol.SolNode solNode : sol.network().liveNodes()) {
//                for (int slot = 1; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
//                    SlotData md = solNode.slotMetaDatas.getPrevious().get(slot);
//                    Assert.assertEquals("Epoch "+epoch+", Slot "+slot, sol.getStakeDistribution().getPrevious().getTotalStake(), md.receivedVotingPower);
//                }
//            }
//        }
//    }

    public void testOnVoteReceiveCurrentEpoch() {
        Solana.SolNode nodeA = sol.network().allNodes.get(0);
        Solana.SolNode nodeB = sol.network().allNodes.get(1);
        nodeB.epoch = 2;
        nodeB.onVoteReceive(nodeA, 2, 27);

        SlotData md = nodeB.slotMetaDatas.getCurrent().get(27);
        Assert.assertNotNull(md);
        Assert.assertEquals(1 , md.txCounterVote);
        Assert.assertEquals(sol.getStakeDistribution().getCurrent().getStake(nodeA.nodeId).nodeStake, md.totalVotingPower);
    }

    public void testOnVoteReceivePreviousEpoch() {
        Solana.SolNode nodeA = sol.network().allNodes.get(0);
        Solana.SolNode nodeB = sol.network().allNodes.get(1);
        nodeB.epoch = 1;
        nodeB.onVoteReceive(nodeA, 2, 27);

        SlotData md = nodeB.slotMetaDatas.getPrevious().get(27);
        Assert.assertNotNull(md);
        Assert.assertEquals(1 , md.txCounterVote);
        Assert.assertEquals(sol.getStakeDistribution().getPrevious().getStake(nodeA.nodeId).nodeStake, md.totalVotingPower);
    }

//    public void testCreateSlotDataForLeaderWithCurrentEpoch() {
//        int epoch = 0;
//        int slot = 37;
//        Sol.SolNode leader = sol.getSlotLeader(slot);
//        leader.epoch = 0;
//        leader.slotMetaDatas.getCurrent().put(slot, new SlotData(1,2,3,4,5));
//
//        leader.createSlotData(epoch, slot, new SlotData(123,2,3,4,5));
//
//        SlotData md = leader.slotMetaDatas.getCurrent().get(slot);
//        Assert.assertNotNull(md);
//        Assert.assertEquals(123, md.txCounterNonVote);
//        Assert.assertEquals(2, md.receivedVotingPower);
//        Assert.assertEquals(3, md.totalVotingPower);
//        Assert.assertEquals(4, md.txCounterVote);
//        Assert.assertEquals(0, md.arriveTime);
//    }

//    public void testCreateSlotDataForLeaderWithPreviousEpoch() {
//        int epoch = 0;
//        int slot = 37;
//        Sol.SolNode leader = sol.getSlotLeader(slot);
//        leader.epoch = 1;
//        leader.slotMetaDatas.getPrevious().put(slot, new SlotData(1,2,3,4,5));
//
//        leader.createSlotData(epoch, slot, new SlotData(123,2,3,4,5));
//
//        SlotData md = leader.slotMetaDatas.getPrevious().get(slot);
//        Assert.assertNotNull(md);
//        Assert.assertEquals(123, md.txCounterNonVote);
//        Assert.assertEquals(2, md.receivedVotingPower);
//        Assert.assertEquals(3, md.totalVotingPower);
//        Assert.assertEquals(4, md.txCounterVote);
//        Assert.assertEquals(0, md.arriveTime);
//    }

    public void testOnSlotEnd() {
        int epoch = 12;
        int slot = 111;
        Solana.SolNode leader = sol.getSlotLeader(slot);
        leader.slotMetaDatas.getCurrent().put(slot, new SlotData(1,2,3,4,5));
        leader.epoch = epoch;

        leader.onSlotEnd(epoch, slot);
        SlotData md = leader.slotMetaDatas.getCurrent().get(slot);
        Assert.assertNotNull(md);
        Assert.assertEquals(TxPerSlot.MEAN, md.txCounterNonVote, TxPerSlot.STD_DEVIATION);
        Assert.assertEquals(2, md.receivedVotingPower);
        Assert.assertEquals(3, md.totalVotingPower);
        Assert.assertEquals(4, md.txCounterVote);
        Assert.assertEquals(5, md.arriveTime);

    }
}
