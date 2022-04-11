package net.consensys.wittgenstein.protocols.ouroboros.output.dto;

public class Leader {
    public int node;
    public int slot;
    public int epoch;
    public boolean byzantine;

    public Leader(int node, int slot, int epoch, boolean byzantine) {
        this.node = node;
        this.slot = slot;
        this.epoch = epoch;
        this.byzantine = byzantine;
    }
}
