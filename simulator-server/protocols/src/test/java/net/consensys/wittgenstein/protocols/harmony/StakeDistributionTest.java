package net.consensys.wittgenstein.protocols.harmony;

import com.google.common.collect.Streams;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;

public class StakeDistributionTest extends TestCase {
    int networkSize;
    int numberOfShards;
    StakeDistribution sd;

    @Override
    protected void setUp() throws Exception {
        networkSize = 600;
        numberOfShards = 4;
        sd = new StakeDistribution(networkSize, numberOfShards, new Random(), 500);
    }

    public void testConstructor() {
        int networkSize = 600;
        int numberOfShards = 4;
        StakeDistribution sd = new StakeDistribution(networkSize, numberOfShards, new Random(), 500);

        //Assert.assertEquals(sd.);
    }

    public void testCalculateTokenSize() {
        int expectedToken = 542;
        int lambda = 600;
        int totalStake = expectedToken * numberOfShards * lambda;
        Assert.assertEquals(expectedToken, sd.calculateSize(totalStake, numberOfShards));
    }

    public void testReadStakeDistributionFromConfigurationFile() {
        Assert.assertEquals(300, sd.readStakeDistributionFromConfigurationFile(300).size());
        Assert.assertEquals(10_000, sd.readStakeDistributionFromConfigurationFile(10_000).size());
    }

    public void testRedistributeToShards() {
        sd.redistributeToShards(42, 0);
        Assert.assertEquals(numberOfShards, sd.shards.size());
        for (int shardId = 0; shardId < numberOfShards; shardId++) {
            Assert.assertEquals(shardId, sd.shards.get(shardId).shardId);
        }
        // test that function gives always same result with the same seed
        List<Integer> epochLeaders = sd.shards.stream().mapToInt(e -> e.epochLeader).boxed().collect(Collectors.toList());
        List<Map<Integer, Long>> stakeholders = sd.shards.stream().map(e -> e.stakeholders).collect(Collectors.toList());
        for (int i = 0; i < 10; i++) {
            sd.redistributeToShards(42, 0);
            for (int shardId = 0; shardId < numberOfShards; shardId++) {
                Assert.assertEquals(epochLeaders.get(shardId).intValue(), sd.shards.get(shardId).epochLeader);
                int finalShardId = shardId;
                Assert.assertTrue(stakeholders.get(shardId).entrySet().stream().allMatch(e -> e.getValue().equals(sd.shards.get(finalShardId).stakeholders.get(e.getKey()))));
            }
        }
    }
}
