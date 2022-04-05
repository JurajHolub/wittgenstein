package net.consensys.wittgenstein.protocols.ouroboros.messages;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.FloodMessage;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.ouroboros.Block;
import net.consensys.wittgenstein.protocols.ouroboros.OuroborosNode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockAnnounce extends FloodMessage<OuroborosNode> {

    public final Block block;

    public BlockAnnounce(Block block) {
        super();
        this.block = block;
    }

    @Override
    public int size() {
        return block.size();
    }
}
