package net.consensys.wittgenstein.protocols.ouroboros;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.core.P2PNetwork;
import net.consensys.wittgenstein.core.messages.FloodMessage;
import net.consensys.wittgenstein.protocols.ouroboros.messages.BlockAnnounce;
import net.consensys.wittgenstein.protocols.ouroboros.output.OutputDumper;

import java.util.List;
import java.util.Random;

public class OuroborosNode extends Ouroboros.AOuroborosNode {

    private Block lastReceivedValidBlock;
    public boolean underDos = false;

    public OuroborosNode(
            Random rd,
            NodeBuilder nb,
            boolean byzantine,
            OuroborosConfig ouroborosConfig,
            P2PNetwork<OuroborosNode> network,
            List<Integer> leaderSchedule,
            OutputDumper outputDumper,
            StakeDistribution stakeDistribution
    ) {
        super(rd, nb, byzantine, ouroborosConfig, network, leaderSchedule, outputDumper, stakeDistribution);
        this.lastReceivedValidBlock = new Block();
    }

    public int generateTransactionsPerBlock() {
        return ouroborosConfig.expectedTxPerBlock + (int) (rd.nextGaussian() * ouroborosConfig.expectedTxPerBlock / 10);
    }

    /**
     * Generate new block and distribute it to the peers.
     */
    public void onSlotEnd(int epoch, int slot) {
        int transactions = generateTransactionsPerBlock();
        Block block = new Block(
            nodeId,
            slot,
            epoch,
            transactions,
            ouroborosConfig.blockHeaderSizeInBytes,
            ouroborosConfig.txSizeInBytes,
            network.time
        );
        BlockAnnounce blockAnnounce = new BlockAnnounce(block);

        network.sendPeers(blockAnnounce, this);
        //network.sendAll(blockAnnounce, this);
    }

    /**
     * Peer receive block and if valid then dump to the database.
     */
    @Override
    public void onFlood(OuroborosNode from, FloodMessage floodMessage) {
        BlockAnnounce blockAnnounce = (BlockAnnounce) floodMessage;
        Block block = blockAnnounce.block;

        if (block.isLessThen(lastReceivedValidBlock)) return;
        if (!isLeader(block.slot, block.creator)) return;

        outputDumper.dumpSlot(block, this, network.time);
        lastReceivedValidBlock = block;
    }

    public boolean isLeader(int slot, int leader) {
        return (ouroborosConfig.vrfLeaderSelection)
                ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(slot) == leader
                : leaderSchedule.get(slot) == leader;
    }
}
