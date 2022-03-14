package net.consensys.wittgenstein.protocols.harmony.output;

import java.util.Map;

public class StakeStats {
    public int node;
    public int stake;
    public int tokens;
    public boolean byzantine;
    public Map<Integer, Long> shardTokens;

    public StakeStats(
        int node,
        int stake,
        int tokens,
        boolean byzantine,
        Map<Integer, Long> shardTokens
    ) {
        this.node = node;
        this.stake = stake;
        this.tokens = tokens;
        this.byzantine = byzantine;
        this.shardTokens = shardTokens;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getStake() {
        return stake;
    }

    public void setStake(int stake) {
        this.stake = stake;
    }

    public int getTokens() {
        return tokens;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public Map<Integer, Long> getShardTokens() {
        return shardTokens;
    }

    public void setShardTokens(Map<Integer, Long> shardTokens) {
        this.shardTokens = shardTokens;
    }
}
