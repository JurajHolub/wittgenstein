package net.consensys.wittgenstein.protocols.harmony.rbs.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;

public class LeaderInit extends Message<HarmonyNode> {
    private final int lastBlockHash;

    public LeaderInit(int lastBlockHash) {
        this.lastBlockHash = lastBlockHash;
    }


    @Override
    public void action(Network<HarmonyNode> network, HarmonyNode from, HarmonyNode to) {
        to.rbs.onLeaderInit(from, lastBlockHash);
    }
}
