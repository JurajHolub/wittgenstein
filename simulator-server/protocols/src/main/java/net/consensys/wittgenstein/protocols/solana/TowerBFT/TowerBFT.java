package net.consensys.wittgenstein.protocols.solana.TowerBFT;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.protocols.solana.Block;
import net.consensys.wittgenstein.protocols.solana.SolanaConfig;
import net.consensys.wittgenstein.protocols.solana.SolanaNode;
import net.consensys.wittgenstein.protocols.solana.StakeDistribution;
import net.consensys.wittgenstein.protocols.solana.TowerBFT.Protocol.SlotAnnounceMessage;
import net.consensys.wittgenstein.protocols.solana.TowerBFT.Protocol.SlotProposeMessage;
import net.consensys.wittgenstein.protocols.solana.TowerBFT.Protocol.VoteMessage;
import net.consensys.wittgenstein.protocols.solana.output.dto.NodeSlot;
import net.consensys.wittgenstein.protocols.solana.output.OutputDumper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TowerBFT voting protocol (based on BFT) used in Solana consensus.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class TowerBFT {

    private transient SolanaNode me;
    private transient StakeDistribution stakeDistribution;
    private transient List<Integer> leaderSchedule;
    private transient Network<SolanaNode> network;
    private transient OutputDumper outputDumper;
    public final Map<Integer, Block> receivedBlocks;
    private SolanaConfig solanaConfig;

    public TowerBFT(
            SolanaNode solanaNode,
            StakeDistribution stakeDistribution,
            List<Integer> leaderSchedule,
            Network<SolanaNode> network,
            OutputDumper outputDumper,
            SolanaConfig solanaConfig
    ) {
        this.me = solanaNode;
        this.stakeDistribution = stakeDistribution;
        this.leaderSchedule = leaderSchedule;
        this.network = network;
        this.outputDumper = outputDumper;
        this.receivedBlocks = new HashMap<>();
        this.solanaConfig = solanaConfig;
    }

    /**
     * 1. Leader propose a new block in PoH sequence.
     */
    public void onSlotPropose(Block block) {
        receivedBlocks.put(block.slot, block);

        network.sendAll(new SlotProposeMessage(block), me);
    }

    /**
     * 2. Validators receive proposed block from leader, validates it and send vote to leader.
     */
    public void onSlotReceive(SolanaNode from, Block block) {
        //if (network.rd.nextDouble() <= solanaConfig.validatorReliability) {
        int leader = solanaConfig.vrfLeaderSelection
                ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(block.slot)
                : leaderSchedule.get(block.slot);
        network.send(new VoteMessage(block), me, network.allNodes.get(leader));
        //}
    }

    /**
     * 3. Leader receive vote and stores it.
     */
    public void onVoteReceive(SolanaNode from, Block block) {
        if (me.underDDoS) return;

        Block leaderBlock = receivedBlocks.get(block.slot);
        leaderBlock.txCounterVote++;
        leaderBlock.receivedVotingPower += stakeDistribution.getStake(from.nodeId).nodeStake;
        receivedBlocks.put(block.slot, leaderBlock);
    }

    /**
     * 4. At the end of slot, leader distribute new block including all signatures (votes). Anyone can check that it is valid.
     */
    public void onSlotEnd(int slot, int epoch) {
        if (me.underDDoS) return;

        Block block = receivedBlocks.get(slot);
        network.sendAll(new SlotAnnounceMessage(block), me);
    }

    /**
     * 5. Validators receive new block, check votes. If valid then it is finalized.
     */
    public void onSlotAnnounce(SolanaNode from, Block block) {
        if (!from.isLeader(block.slot)) return;

        if (!block.isValid(stakeDistribution.getTotalStake())) return;

        outputDumper.dumpSlotState(
            new NodeSlot(
                me.nodeId,
                block,
                network.time,
                me.getMsgReceived(),
                me.getMsgSent(),
                me.getBytesReceived(),
                me.getBytesSent(),
                me.isLeader(block.slot)
            )
        );
        me.cleanStats();
    }

}
