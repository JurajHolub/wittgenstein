package net.consensys.wittgenstein.protocols.harmony;

import net.consensys.wittgenstein.protocols.utils.VRFLeaderSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simulation representation of Harmony shard.
 */
public class Shard {
    /** unique ID. */
    public final int shardId;
    /** Nodes included in this shard */
    public final List<Integer> nodes;
    /** Stake share of nodes in this shard */
    public final Map<Integer, Long> stakeholders;
    /** Shard leader for actual epoch. */
    public final int epochLeader;
    /** ID of beacon shard. */
    public static int BEACON_SHARD = 0;
    /** Leader selection with VRF if no leader schedule in use. */
    public VRFLeaderSelection vrfLeaderSelection;

    public Shard(int epoch, int shardId, List<Integer> tokens) {
        this.shardId = shardId;
        this.epochLeader = tokens.get(0);
        this.stakeholders = tokens.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        this.nodes = new ArrayList<>(stakeholders.keySet());
        long totalTokens = stakeholders.values().stream().mapToLong(i->i).sum();
        List<Double> nodesProbability = new ArrayList<>();
        for (long t : stakeholders.values()) {
            nodesProbability.add(t / (double)totalTokens);
        }
        this.vrfLeaderSelection = new VRFLeaderSelection(epoch, new ArrayList<>(this.stakeholders.keySet()), nodesProbability);
    }

    public Map<Integer, Long> getStakeholders() {
        return stakeholders;
    }
}
