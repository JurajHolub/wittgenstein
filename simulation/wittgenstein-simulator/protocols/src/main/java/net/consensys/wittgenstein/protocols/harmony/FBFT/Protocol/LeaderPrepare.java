package net.consensys.wittgenstein.protocols.harmony.FBFT.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.harmony.BlockSigners;
import net.consensys.wittgenstein.protocols.harmony.HarmonyConfig;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;

public class LeaderPrepare extends Message<HarmonyNode> {
    private final BlockSigners block;

    public LeaderPrepare(BlockSigners block) {
        this.block = block;
    }

    @Override
    public void action(Network<HarmonyNode> network, HarmonyNode from, HarmonyNode to) {
        to.fbft.onLeaderPrepare(from, block);
    }

    @Override
    public int size() {
        return block.size();
    }
}
