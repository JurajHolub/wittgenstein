package net.consensys.wittgenstein.protocols.solana;

import junit.framework.TestCase;
import net.consensys.wittgenstein.core.NetworkLatency;

public class StatsTest extends TestCase {

    private int networkSize;
    private Solana sol;

    @Override
    protected void setUp() throws Exception {
        networkSize = 1000;
        sol = new Solana(networkSize, 3000);
        sol.network().networkLatency = new NetworkLatency.IC3NetworkLatency();
        sol.init();
    }

//    public void testSwapPrevAndCurrentEpoch() {
//
//        sol.simulateEpoch(0);
//        sol.network().runMs(1000);
//
//        List<Integer> nodesEpochEndTime = sol.network().allNodes.stream()
//                .mapToInt(n -> {
//                    SlotData md = n.slotMetaDatas.getPrevious().get(SolanaConfig.EPOCH_DURATION_IN_SLOTS - 1);
//                    Assert.assertNotNull(String.format("Node %d", n.nodeId), md);
//                    return md.arriveTime;
//                }).boxed().collect(Collectors.toList());
//
//        for (int i = 0; i < sol.network().allNodes.size(); i++) {
//            for (int slot = 0; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
//                SlotData curr = sol.network().allNodes.get(i).slotMetaDatas.getCurrent().get(slot);
//                SlotData prev = sol.network().allNodes.get(i).slotMetaDatas.getPrevious().get(slot);
//                Assert.assertNull(curr);
//                Assert.assertNotNull(prev);
//
//                if (slot+1 == SolanaConfig.EPOCH_DURATION_IN_SLOTS) {
//                    int nodeEpochEndTime = nodesEpochEndTime.get(i);
//                    Assert.assertEquals(nodeEpochEndTime, prev.arriveTime);
//                }
//            }
//        }
//    }

//    public void testEpochStats() {
//
//        int numberOfEpochs = 2;
//        sol.simulate(numberOfEpochs);
//        sol.network().runMs(1000);
//
//        for (int i = 0; i < numberOfEpochs; i++) {
//            Assert.assertNotNull(sol.stats.epochs.get(i));
//            Assert.assertEquals(sol.network().allNodes.size(), sol.stats.epochs.get(i).nodeAck);
//            Stats.Epoch e = sol.stats.epochs.get(i);
//            for (int slot = 0; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
////                Assert.assertTrue(String.format("Epoch %d, Slot %d: votes (%d) !<= total voters(%d)",
////                                i, slot, e.slots.get(slot).txVotes, networkSize),
////                        e.slots.get(slot).txVotes <= networkSize);
//            }
//        }
//
//        sol.stats.calculateStats();
//        sol.stats.printStats();
//    }

//    public void testAddEpoch() {
//        Map<Integer, SlotData> slotMds = new HashMap<>();
//        Random r = new Random();
//        for (int slot = 0; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
//            int avgTxCounterVote = r.nextInt(100);
//            int avgReceivedVotingPower = r.nextInt(100);
//            int avgTotalVotingPower = r.nextInt(100);
//            int avgTxCounterNonVote = r.nextInt(100);
//            int avgArriveTime = r.nextInt(100);
//            slotMds.put(slot, new SlotData(avgTxCounterVote, avgReceivedVotingPower, avgTotalVotingPower, avgTxCounterNonVote, avgArriveTime));
//        }
//        SlotData expected = new SlotData(0,0,0,0,0);
//        expected.txCounterVote = slotMds.values().stream().mapToInt(i->i.txCounterVote).sum() / SolanaConfig.EPOCH_DURATION_IN_SLOTS;
//        expected.receivedVotingPower = slotMds.values().stream().mapToInt(i->i.receivedVotingPower).sum() / SolanaConfig.EPOCH_DURATION_IN_SLOTS;
//        expected.totalVotingPower = slotMds.values().stream().mapToInt(i->i.totalVotingPower).sum() / SolanaConfig.EPOCH_DURATION_IN_SLOTS;
//        expected.txCounterNonVote = slotMds.values().stream().mapToInt(i->i.txCounterNonVote).sum() / SolanaConfig.EPOCH_DURATION_IN_SLOTS;
//        expected.arriveTime = slotMds.values().stream().mapToInt(i->i.arriveTime).sum() / SolanaConfig.EPOCH_DURATION_IN_SLOTS;
//
//        sol.stats.addEpoch(0, slotMds);
//
//        SlotData result = sol.stats.epochs.get(0).averageSlotData;
//        Assert.assertEquals(expected.txCounterVote, result.txCounterVote, 1);
//        Assert.assertEquals(expected.receivedVotingPower, result.receivedVotingPower, 1);
//        Assert.assertEquals(expected.totalVotingPower, result.totalVotingPower, 1);
//        Assert.assertEquals(expected.txCounterNonVote, result.txCounterNonVote, 1);
//    }

    public void test(){
        int networkSize = 1000;
        int numberOfEpochs = 5;
        Solana sol = new Solana(networkSize, 3000);
        sol.network().networkLatency = new NetworkLatency.NetworkFixedLatency(100);
        sol.init();
        sol.simulate(numberOfEpochs);
    }

//    public void testCalculateTxStats() {
//        Map<Integer, SlotData> slotMds = new HashMap<>();
//        Random r = new Random();
//        for (int slot = 0; slot < SolanaConfig.EPOCH_DURATION_IN_SLOTS; slot++) {
//            int avgTxCounterVote = r.nextInt(100);
//            int avgReceivedVotingPower = r.nextInt(100);
//            int avgTotalVotingPower = r.nextInt(100);
//            int avgTxCounterNonVote = r.nextInt(100);
//            int avgArriveTime = r.nextInt(100);
//            slotMds.put(slot, new SlotData(avgTxCounterVote, avgReceivedVotingPower, avgTotalVotingPower, avgTxCounterNonVote, avgArriveTime));
//        }
//        sol.stats.addEpoch(0, slotMds);
//        BigInteger tpsTotal = BigInteger.valueOf(0);
//        for (int epoch = 0; epoch < sol.stats.epochs.size(); epoch++) {
//            SlotData averageSlotData = sol.stats.epochs.get(epoch).averageSlotData;
//            int total = averageSlotData.txCounterNonVote + averageSlotData.txCounterVote;
//            tpsTotal.add(BigInteger.valueOf(total));
//        }
//        tpsTotal.d
//
//        sol.stats.calculateTxStats();
//
//        Assert.assertEquals(tpsTotal.intValue(), sol.stats.tpsTotal);
//        Assert.assertEquals(, sol.stats.tpsVote);
//        Assert.assertEquals(, sol.stats.tpsNonVote);
//        Assert.assertEquals(, sol.stats.avgVoteCount);
//        Assert.assertEquals(, sol.stats.avgVoteStakeTotal);
//        Assert.assertEquals(, sol.stats.avgVoteStake);
//    }
}
