package net.consensys.wittgenstein.protocols.harmony.output;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.protocols.harmony.*;
import net.consensys.wittgenstein.protocols.harmony.output.dto.Leader;
import net.consensys.wittgenstein.protocols.harmony.output.dto.OutputInfo;
import net.consensys.wittgenstein.protocols.harmony.output.dto.SlotStats;
import net.consensys.wittgenstein.protocols.harmony.output.dto.StakeStats;
import net.consensys.wittgenstein.protocols.utils.MongoDumper;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.net.UnknownHostException;
import java.util.*;

public class OutputDumper extends MongoDumper {

    private final List<SlotStats> epochSlotStats = new ArrayList<>();
    private final OutputInfo outputInfo = new OutputInfo();
    private HarmonyConfig harmonyConfig;
    private JacksonMongoCollection<SlotStats> collectionSlotStats;
    private JacksonMongoCollection<Leader> collectionLeaders;
    private JacksonMongoCollection<StakeStats> collectionStakeStats;

    public OutputDumper(HarmonyConfig harmonyConfig) throws UnknownHostException {
        super(harmonyConfig);
        this.harmonyConfig = harmonyConfig;
        collectionSlotStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Epochs", SlotStats.class, UuidRepresentation.STANDARD);
        collectionLeaders = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Leaders", Leader.class, UuidRepresentation.STANDARD);
        collectionStakeStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "StakeStats", StakeStats.class, UuidRepresentation.STANDARD);
    }

    public void dumpSlot(SlotStats slotStats) {
        epochSlotStats.add(slotStats);
    }

    public void dumpEpoch() {
        if (epochSlotStats.isEmpty()) return;
        logger.info(String.format("Dump epochs: %d", epochSlotStats.size()));

        collectionSlotStats.insertMany(epochSlotStats);

        epochSlotStats.clear();
    }

    public void dumpLeaders(List<Leader> leaders) {

        collectionLeaders.insertMany(leaders);
    }

    public void dumpEpochStake(int epoch, StakeDistribution stakeDistribution, Network<HarmonyNode> network) {
        List<StakeStats> stakeStats = new ArrayList<>();
        for (int node = 0; node < stakeDistribution.getNodesStake().size(); node++) {
            int stake = stakeDistribution.getNodesStake().get(node);
            int tokens = stakeDistribution.getNodesTokens().get(node);
            boolean byzantine = network.allNodes.get(node).byzantine;
            Map<Integer, Long> shardTokens = new HashMap<>();
            for (Shard shard : stakeDistribution.shards) {
                shardTokens.put(shard.shardId, shard.stakeholders.getOrDefault(node, 0L));
            }
            stakeStats.add(new StakeStats(node, epoch, stake, tokens, byzantine, shardTokens));
        }

        collectionStakeStats.insertMany(stakeStats);
    }

    public OutputInfo outputInfo() {
        return outputInfo;
    }
}
