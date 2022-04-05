package net.consensys.wittgenstein.protocols.solana.output.dto;

public class Stake {
    public int stake;
    public int node;
    public int epoch;

    public Stake(int node, int stake, int epoch) {
        this.stake = stake;
        this.node = node;
        this.epoch = epoch;
    }
}
