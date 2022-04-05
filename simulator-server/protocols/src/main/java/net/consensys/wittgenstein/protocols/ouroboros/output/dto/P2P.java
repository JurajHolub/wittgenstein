package net.consensys.wittgenstein.protocols.ouroboros.output.dto;

import java.util.List;

public class P2P {
    public int node;
    public String city;
    public List<Integer> peers;

    public P2P(int node, String city, List<Integer> peers) {
        this.node = node;
        this.city = city;
        this.peers = peers;
    }
}
