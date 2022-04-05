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

    public void onSlotEnd(int epoch, int slot) {
        // 3. Ak je uživateľ aktuálny vodca, tak vytvorí nový blok z~transakcií ktoré získal. Blok pridá na koniec BC a distribuuje ho. Je dôležité podotknúť, že vodca nemusí vždy publikovať nový blok a teda slot môže byť "prázdny". Avšak jeden slot môže publikovať maximálne jeden blok.

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

//    public void onBlockReceive(OuroborosNode from, Block block) {
//        if (block.slot <= lastReceivedValidBlock.slot) return;
//        if (block.epoch < lastReceivedValidBlock.epoch) return;
//        if (!isLeader(block.slot, block.creator)) return;
//
//        outputDumper.dumpSlot(block, this, network.time);
//
//        lastReceivedValidBlock = block;
//
//    }

    @Override
    public void onFlood(OuroborosNode from, FloodMessage floodMessage) {
        BlockAnnounce blockAnnounce = (BlockAnnounce) floodMessage;
        Block block = blockAnnounce.block;
        if (block.slot <= lastReceivedValidBlock.slot) return;
        if (block.epoch < lastReceivedValidBlock.epoch) return;
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
