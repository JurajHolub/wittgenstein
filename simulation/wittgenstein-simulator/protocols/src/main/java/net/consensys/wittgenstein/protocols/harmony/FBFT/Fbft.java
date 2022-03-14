package net.consensys.wittgenstein.protocols.harmony.FBFT;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.protocols.harmony.*;
import net.consensys.wittgenstein.protocols.harmony.FBFT.Protocol.*;
import net.consensys.wittgenstein.protocols.harmony.output.SlotStats;

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

    public Fbft(Random rd, NodeBuilder nb, Network<HarmonyNode> network, HarmonyNode me, StakeDistribution stakeDistribution, Logger logger, HarmonyConfig  harmonyConfig) {
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

    // 1. Vodca vytvorí blok a rozošle jeho hlavičku aj dátový obsah validátorom pomocou
    // broadcastu.
    public void onBlockCreate(int epoch, int slot, int shard) {
        Block block;
        int transactions = me.generateTransactionsPerBlock();
        block = new Block(shard, epoch, slot, transactions, pseudoRand.poll(), rand.poll(), harmonyConfig.blockHeaderSizeInBytes, harmonyConfig.txSizeInBytes);

        LeaderAnnounce leaderAnnounce = new LeaderAnnounce(block);
        List<HarmonyNode> shardNodes = getShardNodes(shard);
        network.send(leaderAnnounce, me, shardNodes);
    }

    // 2. Validátori príjmu nový blok, overia jeho hlavičku, podpíšu ju svojím digitálnym podpisu
    // a pošlú späť vodcovi. Obsah bloku je zatiaľ ignorovaný.
    public void onLeaderAnnounce(HarmonyNode leader, Block block) {
        if (!block.isHeaderValid()) return;

        ValidatorAnnounce validatorAnnounce = new ValidatorAnnounce(block);
        network.send(validatorAnnounce, me, leader);
    }

    // 3. Keď vodca prijme aspoň 2/3 podpisov, tak ich agreguje do jediného prahového digitálneho
    // podpisu. Tento podpis rozošle pomocou broadcastu spolu s bitmapou indikujúcou validátorov
    // ktorý podpísali.
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

    // 4. Každý validátor overí, že prahový podpis obsahuje požadované 2/3 hlasov. Až v tejto
    // chvíli validátor overí transakcie v dátovom obsahu bloku, ktorý bol zasielaný už
    // v kroku 1. Ak všetko súhlasí, tak podpíše správu s kroku 3 a pošle ju späť vodcovi.
    public void onLeaderPrepare(HarmonyNode leader, BlockSigners block) {
        if (!block.majoritySigned()) return;
        epochPrepare.put(block.slot, block.shard, block);

        if (!block.isDataValid()) return;

        ValidatorPrepare validatorPrepare = new ValidatorPrepare(block);
        network.send(validatorPrepare, me, leader);
    }

    // 5. Vodca čaká na 2/3 podpisov validátorov s predošlého kroku (môžu sa líšiť od podpisov
    // z kroku 3). Opäť ich agreguje do prahového podpisu a spolu s bitmapou účastníkov
    // rozošle pomocou broadcastu nový blok na potvrdenie všetkým validátorom.
    public void onValidatorPrepare(HarmonyNode validator, BlockSigners block) {
        if (isBlockSent(block, false)) return;

        addSignature(block, false, validator);

        if (epochCommit.get(block.slot, block.shard).majoritySigned()) {
            Commit commit = new Commit(epochCommit.get(block.slot, block.shard));
            network.send(commit, me, getShardNodes(block.getShard()));
            markBlockAsSent(block, false);
        }
    }

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

    public void saveStatsAboutSlot(Block block) {
        me.csvDumper.dumpSlot(
            new SlotStats(
                me.nodeId,
                block.shard,
                block.slot,
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
