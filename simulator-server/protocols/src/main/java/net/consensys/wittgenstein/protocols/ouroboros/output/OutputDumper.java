package net.consensys.wittgenstein.protocols.ouroboros.output;

import net.consensys.wittgenstein.protocols.ouroboros.Block;
import net.consensys.wittgenstein.protocols.ouroboros.OuroborosConfig;
import net.consensys.wittgenstein.protocols.ouroboros.OuroborosNode;
import net.consensys.wittgenstein.protocols.utils.MongoDumper;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OutputDumper extends MongoDumper {

    private OuroborosConfig ouroborosConfig;
    private JacksonMongoCollection<Slot> collectionSlotStats;
    private JacksonMongoCollection<Leader> collectionLeaders;
    private JacksonMongoCollection<Stake> collectionStakeStats;
    private JacksonMongoCollection<P2P> collectionP2PNetwork;
    private List<Slot> slotPool = new ArrayList<>();


    public OutputDumper(OuroborosConfig ouroborosConfig) throws UnknownHostException {
        super(ouroborosConfig);
        this.ouroborosConfig = ouroborosConfig;

        collectionSlotStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Epochs", Slot.class, UuidRepresentation.STANDARD);
        collectionLeaders = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Leaders", Leader.class, UuidRepresentation.STANDARD);
        collectionStakeStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Stake", Stake.class, UuidRepresentation.STANDARD);
        collectionP2PNetwork = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "P2PNetwork", P2P.class, UuidRepresentation.STANDARD);
    }

    public void dumpSlot(Block block, OuroborosNode node, long time) {
        Slot slot = new Slot(
            block,
            node.nodeId,
            time,
            node.getMsgReceived(),
            node.getMsgSent(),
            node.getBytesReceived(),
            node.getBytesSent(),
            0
        );
        slotPool.add(slot);
        if (slotPool.size() < 100) return;
        collectionSlotStats.insertMany(slotPool);
        slotPool.clear();
    }

    public void dumpLeaderSchedule(int epoch, List<Integer> leaders) {
        List<Leader> leadersDto = new ArrayList<>();
        for (int slot = 0; slot < leaders.size(); slot++) {
            leadersDto.add(new Leader(leaders.get(slot), slot, epoch));
        }
        collectionLeaders.insertMany(leadersDto);
    }

    public void dumpStake(int epoch, List<Integer> nodesStake) {
        List<Stake> stakeDto = new ArrayList<>();
        for (int node = 0; node < nodesStake.size(); node++) {
            nodesStake.get(node);
            stakeDto.add(new Stake(nodesStake.get(node), node, epoch));
        }
        collectionStakeStats.insertMany(stakeDto);
    }

    public void dumpP2PNetwork(List<OuroborosNode> nodes) {
        collectionP2PNetwork.insertMany(
            nodes.stream().map(node ->
                new P2P(
                    node.nodeId,
                    node.cityName,
                    node.peers.stream().map(p->p.nodeId).collect(Collectors.toList())
                )
            ).collect(Collectors.toList())
        );
    }
}
