package net.consensys.wittgenstein.protocols.solana.output;

import net.consensys.wittgenstein.protocols.solana.SolanaConfig;
import net.consensys.wittgenstein.protocols.solana.SolanaNode;
import net.consensys.wittgenstein.protocols.solana.StakeDistribution;
import net.consensys.wittgenstein.protocols.solana.output.dto.Leaders;
import net.consensys.wittgenstein.protocols.solana.output.dto.NodeSlot;
import net.consensys.wittgenstein.protocols.solana.output.dto.Stake;
import net.consensys.wittgenstein.protocols.utils.MongoDumper;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility helps to send data continuously into the database.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class OutputDumper extends MongoDumper {

    private List<NodeSlot> nodeSlots = new ArrayList<>();
    private SolanaConfig solanaConfig;

    private JacksonMongoCollection<NodeSlot> collectionSlotStats;
    private JacksonMongoCollection<Leaders> collectionLeaders;
    private JacksonMongoCollection<Stake> collectionStakeStats;

    public OutputDumper(SolanaConfig solanaConfig) throws UnknownHostException {
        super(solanaConfig);
        this.solanaConfig = solanaConfig;

        collectionSlotStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Epochs", NodeSlot.class, UuidRepresentation.STANDARD);
        collectionLeaders = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Leaders", Leaders.class, UuidRepresentation.STANDARD);
        collectionStakeStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Stake", Stake.class, UuidRepresentation.STANDARD);
    }

    public void dumpSlotState(NodeSlot nodeSlot) {
        nodeSlots.add(nodeSlot);

        if (nodeSlots.size() >= 100) {
            collectionSlotStats.insertMany(nodeSlots);
            nodeSlots.clear();
        }
    }

    public void dumpStakeState(int epoch, StakeDistribution stakeDistribution, List<Integer> leaderSchedule, List<SolanaNode> nodes) {
        List<Stake> stake = nodes.stream()
                .map(n -> new Stake(n.nodeId, stakeDistribution.getStake(n.nodeId).nodeStake, epoch))
                .collect(Collectors.toList());
        collectionStakeStats.insertMany(stake);

        List<Leaders> leaders = IntStream.range(0, solanaConfig.epochDurationInSlots)
            .mapToObj(slot -> {
                int leaderId = (solanaConfig.vrfLeaderSelection)
                        ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(slot)
                        : leaderSchedule.get(slot);
                return new Leaders(slot, leaderId, nodes.get(leaderId).underDDoS);
            })
            .collect(Collectors.toList());
        collectionLeaders.insertMany(leaders);
    }
}
