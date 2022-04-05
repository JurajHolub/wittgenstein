package net.consensys.wittgenstein.protocols.solana.output.dto;

public class Leaders {
    public int slot;
    public int leaderNode;
    public boolean underDdos;

    public Leaders(int slot, int leaderNode, boolean underDdos) {
        this.slot = slot;
        this.leaderNode = leaderNode;
        this.underDdos = underDdos;
    }
}
