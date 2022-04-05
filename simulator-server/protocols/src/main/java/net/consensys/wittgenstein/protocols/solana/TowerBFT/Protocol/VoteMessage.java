package net.consensys.wittgenstein.protocols.solana.TowerBFT.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.solana.Block;
import net.consensys.wittgenstein.protocols.solana.SolanaNode;

import static net.consensys.wittgenstein.protocols.solana.SolanaConfig.RSA_SIGNATURE_SIZE_IN_BYTES;

public class VoteMessage extends Message<SolanaNode> {
    final Block block;

    public VoteMessage(Block block) {
        this.block = block;
    }

    @Override
    public void action(Network<SolanaNode> network, SolanaNode from, SolanaNode to) {
        to.towerBFT.onVoteReceive(from, block);
    }

    @Override
    public int size() {
        return RSA_SIGNATURE_SIZE_IN_BYTES;
    }
}
