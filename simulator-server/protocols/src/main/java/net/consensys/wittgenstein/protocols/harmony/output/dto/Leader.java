package net.consensys.wittgenstein.protocols.harmony.output.dto;

public class Leader {
    public int node;
    public int epoch;
    public int shard;

    public Leader(int node, int epoch, int shard) {
        this.node = node;
        this.epoch = epoch;
        this.shard = shard;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }
}
