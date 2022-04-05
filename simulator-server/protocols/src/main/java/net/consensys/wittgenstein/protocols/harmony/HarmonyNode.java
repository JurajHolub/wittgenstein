package net.consensys.wittgenstein.protocols.harmony;

import com.google.common.collect.HashBasedTable;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.protocols.harmony.output.OutputDumper;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class HarmonyNode extends Harmony.AHarmonyNode {

    public HarmonyNode(
        Random rd,
        NodeBuilder nb,
        Network<HarmonyNode> network,
        StakeDistribution stakeDistribution,
        HarmonyConfig harmonyConfig,
        Logger logger,
        OutputDumper outputDumper,
        int expectedTps,
        boolean byzantine
    ) {
        super(rd, nb, network, stakeDistribution, harmonyConfig, logger, outputDumper, expectedTps, byzantine);
    }

    public boolean isLeader(Block block) {
        if (harmonyConfig.vrfLeaderSelection) {
            int slotLeader = stakeDistribution.shards.get(block.shard).vrfLeaderSelection.chooseSlotLeader(block.slot);
            return nodeId == slotLeader;
        }

        return stakeDistribution.shards.get(block.shard).epochLeader == nodeId;
    }

    public int generateTransactionsPerBlock() {
        return expectedTxPerBlock + (int) (network.rd.nextGaussian() * (expectedTxPerBlock / 10));
    }

    public void onSlot(int epoch, int slot, int shard) {

        fbft.onBlockCreate(epoch, slot, shard);

    }

    public void cleanStats() {
        msgReceived = 0;
        msgSent = 0;
        fbft.epochPrepare = HashBasedTable.create();
        fbft.epochCommit = HashBasedTable.create();
        rbs.randomNumberCollector.clear();
    }

    public void onDistributedRandomnessGeneration(int epoch, int lastSlot) {
        Block lastBlock = fbft.epochCommit.get(lastSlot, Shard.BEACON_SHARD);
        List<HarmonyNode> nodes = network.allNodes.stream()
            .filter(node -> stakeDistribution.shards.get(Shard.BEACON_SHARD).nodes.contains(node.nodeId))
            .collect(Collectors.toList());
        rbs.onDRG(lastBlock, nodes);
    }

    public boolean isBlockValid(int block) {
        return false;
    }



}