package net.consensys.wittgenstein.protocols.solana;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.protocols.solana.output.OutputDumper;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static net.consensys.wittgenstein.protocols.solana.SolanaConfig.RSA_SIGNATURE_SIZE_IN_BYTES;

public class SolanaNode extends Solana.ASolanaNode {

    public boolean underDDoS = false;

    public SolanaNode(
            Random rd,
            NodeBuilder nb,
            Network<SolanaNode> network,
            StakeDistribution stakeDistribution,
            SolanaConfig solanaConfig,
            Logger logger,
            boolean byzantine,
            List<Integer> leaderSchedule,
            OutputDumper stats
    ) {
        super(rd, nb, network, stakeDistribution, solanaConfig, logger, byzantine, leaderSchedule, stats);
    }

    public void onSlotMessage(int epoch, int slot) {
        if (!isLeader(slot)) return;
        if (underDDoS) return;

        Block block = new Block(
            slot,
            epoch,
            0,
            0,
            generateTransactionsPerBlock(),
            network.time,
            solanaConfig.txSizeInBytes,
            RSA_SIGNATURE_SIZE_IN_BYTES
        );

        towerBFT.onSlotPropose(block);
    }

    public void onSlotEnd(int slot, int epoch) {
        towerBFT.onSlotEnd(slot, epoch);
    }

    public int generateTransactionsPerBlock() {
        return solanaConfig.expectedTxPerBlock + (int) (network.rd.nextGaussian() * (solanaConfig.expectedTxPerBlock / 10));
    }

    public boolean isLeader(int slot) {
        return (solanaConfig.vrfLeaderSelection)
                ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(slot) == nodeId
                : leaderSchedule.get(slot) == nodeId;
    }

    public void cleanStats() {
        msgReceived = 0;
        msgSent = 0;
        bytesReceived = 0;
        bytesSent = 0;
    }
}
