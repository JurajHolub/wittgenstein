package net.consensys.wittgenstein.protocols.harmony;

import com.google.common.collect.Lists;
import net.consensys.wittgenstein.protocols.harmony.output.dto.Leader;
import net.consensys.wittgenstein.protocols.utils.StakeDistributionUtil;

import java.util.*;
import java.util.stream.Collectors;

public class StakeDistribution {

    private final String CONFIGURATION_FILE = "HarmonyStake-2022-02-24.csv";
    private StakeDistributionUtil stakeDistributionUtil;
    private List<Integer> nodesStake;
    private List<Integer> nodesTokens;
    public int totalStake;
    private final HarmonyConfig harmonyConfig;
    private int tokenSize;
    private final Random rd;
    public List<Shard> shards = new ArrayList<>();
    public Queue<Integer> randForNextEpoch = new LinkedList<>();
    public final List<Leader> leaders = new ArrayList<>();

    public StakeDistribution(Random rd, HarmonyConfig harmonyConfig) {
        this.harmonyConfig = harmonyConfig;
        this.rd = rd;
        this.stakeDistributionUtil = new StakeDistributionUtil(rd);
        this.nodesStake = (harmonyConfig.uniformStakeDistribution)
                ? stakeDistributionUtil.uniformDistribution(harmonyConfig.networkSize)
                : stakeDistributionUtil.readStakeDistributionFromConfigurationFile(CONFIGURATION_FILE, harmonyConfig.networkSize); //uniformDistribution(harmonyConfig.networkSize);//readStakeDistributionFromConfigurationFile(networkSize);
        this.totalStake = nodesStake.stream().mapToInt(value -> value).sum();
        this.tokenSize = calculateSize(totalStake, harmonyConfig.numberOfShards);
        this.nodesTokens = nodesStake.stream().mapToInt(stake -> stake / tokenSize).boxed().collect(Collectors.toList());
    }

    public static class Stake {
        public int tokens;
        public int stake;

        public Stake(int tokens, int stake) {
            this.tokens = tokens;
            this.stake = stake;
        }
    }

    public void setLambda(int lambda) {
        harmonyConfig.lambda = lambda;
    }

    public List<Integer> getNodesStake() {
        return nodesStake;
    }

    public List<Integer> getNodesTokens() {
        return nodesTokens;
    }

    public Shard getBeaconShard() {
        return shards.get(0);
    }

    public List<Integer> getShardNodes(int shardId) {
        return shards.get(shardId).nodes;
    }

    public void redistributeToShards(int rnd, int epoch) {
        List<Integer> allTokens = new ArrayList<>();
        for (int node = 0; node < nodesTokens.size(); node++) {
            for (int token = 0; token < nodesTokens.get(node); token++) {
                allTokens.add(node);
            }
        }
        Collections.shuffle(allTokens, new Random(rnd));
        int shardId = 0;
        shards.clear();
        for (List<Integer> tokens : Lists.partition(allTokens, allTokens.size() / harmonyConfig.numberOfShards + 1)) {
            shards.add(new Shard(epoch, shardId++, tokens));
        }
        for (Shard shard : shards) {
            leaders.add(new Leader(shard.epochLeader, epoch, shard.shardId));
        }
    }

    public int calculateSize(int totalStake, int numberOfShards) {
        return (int) ((double)totalStake / (numberOfShards * harmonyConfig.lambda));
    }

    public Stake getStake(int node) {
        return new Stake(
            nodesStake.get(node) / tokenSize,
            nodesStake.get(node)
        );
    }

    /** Simulate redistribution of stake after each epoch. Real stake distribution would depend on transactions
     * included into current epoch, but we do not simulate data layer. As a result, we cannot change stake distribution
     * from real data in transaction.
     */
    public void updateStakeDistribution(int epoch) {
        nodesStake = stakeDistributionUtil.updateStakeDistribution(epoch, nodesStake);

        totalStake = nodesStake.stream().mapToInt(value -> value).sum();
        tokenSize = calculateSize(totalStake, harmonyConfig.numberOfShards);
        nodesTokens = nodesStake.stream().mapToInt(stake -> stake / tokenSize).boxed().collect(Collectors.toList());
    }
}
