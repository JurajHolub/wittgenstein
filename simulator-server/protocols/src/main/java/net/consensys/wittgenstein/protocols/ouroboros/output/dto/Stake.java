package net.consensys.wittgenstein.protocols.ouroboros.output.dto;

public class Stake {
    public int stake;
    public int node;
    public int epoch;

    public Stake(int stake, int node, int epoch) {
        this.stake = stake;
        this.node = node;
    }
}