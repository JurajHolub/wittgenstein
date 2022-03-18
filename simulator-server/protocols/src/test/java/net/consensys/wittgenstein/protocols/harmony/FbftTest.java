package net.consensys.wittgenstein.protocols.harmony;

import junit.framework.TestCase;
import net.consensys.wittgenstein.core.NetworkLatency;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class FbftTest extends TestCase {
    private Harmony harmony;
    int networkSize;
    int numberOfShards;

    @Override
    protected void setUp() throws Exception {
        networkSize = 200;
        numberOfShards = 4;
        harmony = new Harmony(networkSize, numberOfShards, 500, 200, 0);
        harmony.network().networkLatency = new NetworkLatency.NetworkFixedLatency(1);
        harmony.init();
    }

    public void testProtocolForSingleShard() {
        int epoch = 0, slot = 0;

        Shard shard = harmony.stakeDistribution.shards.get(0);
        HarmonyNode leader = harmony.network().allNodes.get(shard.epochLeader);
        harmony.network().run(2);
        leader.onSlot(epoch, slot, shard.shardId);
        harmony.network().run(2);

        List<HarmonyNode> shardNodes = harmony.stakeDistribution.shards.get(0).nodes.stream().map(id-> harmony.network().allNodes.get(id)).collect(Collectors.toList());
        for (HarmonyNode validator : shardNodes) {
            Assert.assertNotNull(validator.fbft.epochPrepare.get(slot, shard.shardId));
            Assert.assertNotNull(validator.fbft.epochCommit.get(slot, shard.shardId));
            Assert.assertTrue(validator.fbft.epochPrepare.get(slot, shard.shardId).majoritySigned());
            Assert.assertTrue(validator.fbft.epochCommit.get(slot, shard.shardId).majoritySigned());
        }
    }

    public void testProtocolForAllShards() {
        int epoch = 0, slot = 0;

        harmony.network().run(2);
        for (Shard shard : harmony.stakeDistribution.shards) {
            HarmonyNode leader = harmony.network().allNodes.get(shard.epochLeader);
            leader.onSlot(epoch, slot, shard.shardId);
        }
        harmony.network().run(2);

        for (Shard shard : harmony.stakeDistribution.shards) {
            List<HarmonyNode> shardNodes = shard.nodes.stream().map(id-> harmony.network().allNodes.get(id)).collect(Collectors.toList());
            for (HarmonyNode validator : shardNodes) {
                Assert.assertNotNull(validator.fbft.epochPrepare.get(slot, shard.shardId));
                Assert.assertNotNull(validator.fbft.epochCommit.get(slot, shard.shardId));
                Assert.assertTrue(validator.fbft.epochPrepare.get(slot, shard.shardId).majoritySigned());
                Assert.assertTrue(validator.fbft.epochCommit.get(slot, shard.shardId).majoritySigned());
            }
        }
    }
}
