package net.consensys.wittgenstein.protocols.harmony.FBFT;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.protocols.harmony.*;
import net.consensys.wittgenstein.protocols.harmony.FBFT.Protocol.*;
import net.consensys.wittgenstein.protocols.harmony.output.dto.SlotStats;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Fbft {

    protected final transient Network<HarmonyNode> network;
    protected final transient HarmonyNode me;
    protected final transient StakeDistribution stakeDistribution;
    protected final transient Logger logger;
    protected final transient HarmonyConfig harmonyConfig;
    public Table<Integer, Integer, BlockSigners> epochPrepare = HashBasedTable.create();
    public Table<Integer, Integer, BlockSigners> epochCommit = HashBasedTable.create();
    Queue<Integer> pseudoRand = new LinkedList<>();
    Queue<Integer> rand = new LinkedList<>();

    public Fbft(Network<HarmonyNode> network, HarmonyNode me, StakeDistribution stakeDistribution, Logger logger, HarmonyConfig  harmonyConfig) {
        this.network = network;
        this.me = me;
        this.stakeDistribution = stakeDistribution;
        this.logger = logger;
        this.harmonyConfig = harmonyConfig;
    }

    private void addSignature(Block block, boolean preparePhase, HarmonyNode validator) {
        BlockSigners blockSigners = new BlockSigners(block, getShardNodes(block.shard).size());
        if (preparePhase) {
            if (epochPrepare.get(block.slot, block.shard) == null) {
                epochPrepare.put(block.slot, block.shard, blockSigners);
            }
            else {
                blockSigners = epochPrepare.get(block.slot, block.shard);
                blockSigners.sign(validator.nodeId);
                epochPrepare.put(block.slot, block.shard, blockSigners);
            }
        }
        else { // commit phase
            if (epochCommit.get(block.slot, block.shard) == null) {
                epochCommit.put(block.slot, block.shard, blockSigners);
            }
            else {
                blockSigners = epochCommit.get(block.slot, block.shard);
                blockSigners.sign(validator.nodeId);
                epochCommit.put(block.slot, block.shard, blockSigners);
            }
        }
    }

    private List<HarmonyNode> getShardNodes(int shardId) {
        return stakeDistribution.getShardNodes(shardId).stream().map(network.allNodes::get).collect(Collectors.toList());
    }

    public void onPseudoRand(int pRand) {
        pseudoRand.add(pRand);
    }

    /**
     * 1. The leader creates the block and sends its header and data content to the validator via broadcast.
     */
    public void onBlockCreate(int epoch, int slot, int shard) {
        Block block;
        int transactions = me.generateTransactionsPerBlock();
        block = new Block(shard, epoch, slot, transactions, pseudoRand.poll(), rand.poll(), harmonyConfig.blockHeaderSizeInBytes, harmonyConfig.txSizeInBytes);

        LeaderAnnounce leaderAnnounce = new LeaderAnnounce(block);
        List<HarmonyNode> shardNodes = getShardNodes(shard);
        network.send(leaderAnnounce, me, shardNodes);
    }

    /**
     * 2. The validators receive the new block, verify its header, sign it with their digital signature
     * and send it back to the leader. The contents of the block are still ignored.
     */
    public void onLeaderAnnounce(HarmonyNode leader, Block block) {
        if (!block.isHeaderValid()) return;

        ValidatorAnnounce validatorAnnounce = new ValidatorAnnounce(block);
        network.send(validatorAnnounce, me, leader);
    }

    /**
     * 3. When a leader receives at least 2/3 of the signatures, he aggregates them into a single digital threshold.
     * It broadcasts this signature along with a bitmap indicating the validators they signed.
     */
    public void onValidatorAnnounce(HarmonyNode validator, BlockSigners block) {
        if (isBlockSent(block, true)) return;

        addSignature(block, true, validator);

        if (epochPrepare.get(block.slot, block.shard).majoritySigned()) {
            LeaderPrepare leaderPrepare = new LeaderPrepare(epochPrepare.get(block.slot, block.shard));
            network.send(leaderPrepare, me, getShardNodes(block.getShard()));
            markBlockAsSent(block, true);
        }
    }

    private void markBlockAsSent(Block block, boolean preparePhase) {
        if (preparePhase) {
            BlockSigners bs = epochPrepare.get(block.slot, block.shard);
            bs.sent = true;
            epochPrepare.put(block.slot, block.shard, bs);
        }
        else {

            BlockSigners bs = epochCommit.get(block.slot, block.shard);
            bs.sent = true;
            epochCommit.put(block.slot, block.shard, bs);
        }
    }

    private boolean isBlockSent(Block block, boolean preparePhase) {
        if (preparePhase) {
            return epochPrepare.get(block.slot, block.shard) != null && epochPrepare.get(block.slot, block.shard).sent;
        }
        else {
            return epochCommit.get(block.slot, block.shard) != null && epochCommit.get(block.slot, block.shard).sent;
        }
    }

    /**
     * 4. Each validator verifies that the threshold signature contains the required 2/3 votes. Only at this point
     * does the validator verify the transactions in the data content of the block that was already sent in step 1.
     * If everything agrees, it will sign the message with step 3 and send it back to the leader.
     */
    public void onLeaderPrepare(HarmonyNode leader, BlockSigners block) {
        if (!block.majoritySigned()) return;
        epochPrepare.put(block.slot, block.shard, block);

        if (!block.isDataValid()) return;

        ValidatorPrepare validatorPrepare = new ValidatorPrepare(block);
        network.send(validatorPrepare, me, leader);
    }

    /**
     * 5. The leader waits for 2/3 of the validator signatures from the previous step (they may differ from the
     * signatures from step 3). Again, it aggregates them into a threshold signature and, together with the subscriber
     * bitmap, broadcasts a new block for confirmation by all validators.
     */
    public void onValidatorPrepare(HarmonyNode validator, BlockSigners block) {
        if (isBlockSent(block, false)) return;

        addSignature(block, false, validator);

        if (epochCommit.get(block.slot, block.shard).majoritySigned()) {
            Commit commit = new Commit(epochCommit.get(block.slot, block.shard));
            network.send(commit, me, getShardNodes(block.getShard()));
            markBlockAsSent(block, false);
        }
    }

    /**
     * 6. Block is finaliszed for this node. Save it to the database.
     */
    public void onCommit(HarmonyNode leader, BlockSigners block) {
        if (!leader.isLeader(block)) return;

        epochCommit.put(block.slot, block.shard, block);
        saveStatsAboutSlot(block);

        if (!me.isLeader(block)) return;

        if (block.pRand != null) {
            logger.info(String.format("Received pRand=%d, node %d, epoch %d, slot %d, shard %d", block.pRand, me.nodeId, block.epoch, block.slot, block.shard));
            int rand = network.rd.nextInt() ^ block.pRand; // simulate calculation of final rand
            IntStream.range(0, harmonyConfig.vdfInSlots -1).forEach(i -> this.rand.add(null)); // simulate vdf that takes N blocks
            this.rand.add(rand);
        }

        if (block.rnd != null) {
            logger.info(String.format("Received rand=%d, node %d, epoch %d, slot %d, shard %d", block.rnd, me.nodeId, block.epoch, block.slot, block.shard));
            stakeDistribution.randForNextEpoch.add(block.rnd);
        }
    }

    /**
     * Helper function for dumping node status for particular slot into the database (mongo).
     */
    public void saveStatsAboutSlot(Block block) {
        me.outputDumper.dumpSlot(
            new SlotStats(
                me.nodeId,
                block.shard,
                block.slot,
                block.epoch,
                block.transactions,
                network.time,
                me.getMsgReceived(),
                me.getMsgSent(),
                me.getBytesReceived(),
                me.getBytesSent()
            )
        );
    }
}
