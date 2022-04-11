package net.consensys.wittgenstein.protocols.ouroboros.output.dto;

public class Stake {
    public int stake;
    public int node;
    public int epoch;
    public boolean byzantine;

    public Stake(int stake, int node, int epoch, boolean byzantine) {
        this.stake = stake;
        this.node = node;
        this.epoch = epoch;
        this.byzantine = byzantine;
    }
}