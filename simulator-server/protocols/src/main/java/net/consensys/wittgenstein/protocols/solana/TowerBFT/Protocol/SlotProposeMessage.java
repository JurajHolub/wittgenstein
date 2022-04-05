package net.consensys.wittgenstein.protocols.solana.TowerBFT.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.solana.Block;
import net.consensys.wittgenstein.protocols.solana.SolanaNode;

public class SlotProposeMessage extends Message<SolanaNode> {
    final Block block;

    public SlotProposeMessage(Block block) {
        this.block = block;
    }

    @Override
    public void action(Network<SolanaNode> network, SolanaNode from, SolanaNode to) {
        to.towerBFT.onSlotReceive(from, block);
    }

    @Override
    public int size() {
        return block.size();
    }
}