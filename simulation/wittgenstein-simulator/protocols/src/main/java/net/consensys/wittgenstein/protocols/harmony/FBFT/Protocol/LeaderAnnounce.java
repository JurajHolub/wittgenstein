package net.consensys.wittgenstein.protocols.harmony.FBFT.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.harmony.Block;
import net.consensys.wittgenstein.protocols.harmony.HarmonyConfig;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;

public class LeaderAnnounce extends Message<HarmonyNode> {
    private final Block block;

    public LeaderAnnounce(Block block) {
        this.block = block;
    }

    @Override
    public void action(Network<HarmonyNode> network, HarmonyNode from, HarmonyNode to) {
        to.fbft.onLeaderAnnounce(from, block);
    }

    @Override
    public int size() {
        return block.size();
    }
}
