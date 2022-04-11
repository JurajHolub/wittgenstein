package net.consensys.wittgenstein.protocols.ouroboros;

import com.google.common.collect.Lists;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.core.P2PNetwork;
import net.consensys.wittgenstein.core.messages.FloodMessage;
import net.consensys.wittgenstein.protocols.harmony.Shard;
import net.consensys.wittgenstein.protocols.harmony.output.dto.Leader;
import net.consensys.wittgenstein.protocols.ouroboros.messages.BlockAnnounce;
import net.consensys.wittgenstein.protocols.ouroboros.output.OutputDumper;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.Collections;
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

    public Block createBlock(int slot, int epoch) {
        int transactions = generateTransactionsPerBlock();
        int hash = rd.nextInt();
        Block block = new Block(
            nodeId,
            slot,
            epoch,
            transactions,
            ouroborosConfig.blockHeaderSizeInBytes,
            ouroborosConfig.txSizeInBytes,
            network.time,
            hash
        );
        return block;
    }

    /**
     * Generate new block and distribute it to the peers.
     */
    public void onSlotEnd(int epoch, int slot) {
        // Fork attack
        if (byzantine) {
            List<OuroborosNode> myPeers = new ArrayList<>(peers);
            Collections.shuffle(myPeers, rd);
            int forks = ouroborosConfig.forkRatio - rd.nextInt(ouroborosConfig.forkRatio-1);
            for (List<OuroborosNode> peerPartition : Lists.partition(myPeers, myPeers.size() / forks + 1)) {
                Block block = createBlock(slot, epoch);
                network.send(new BlockAnnounce(block), this, peerPartition);
            }
        }
        else {
            Block block = createBlock(slot, epoch);
            network.sendPeers(new BlockAnnounce(block), this);
        }
    }

    /**
     * Peer receive block and if valid then dump to the database.
     */
    @Override
    public void onFlood(OuroborosNode from, FloodMessage floodMessage) {
        BlockAnnounce blockAnnounce = (BlockAnnounce) floodMessage;
        Block block = blockAnnounce.block;

        if (block.isLessEqualThen(lastReceivedValidBlock)) return;
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
