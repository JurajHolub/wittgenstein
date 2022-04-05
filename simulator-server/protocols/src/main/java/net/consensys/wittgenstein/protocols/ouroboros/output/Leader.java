package net.consensys.wittgenstein.protocols.ouroboros.output;

public class Leader {
    public int node;
    public int slot;
    public int epoch;

    public Leader(int node, int slot, int epoch) {
        this.node = node;
        this.slot = slot;
        this.epoch = epoch;
    }
}
