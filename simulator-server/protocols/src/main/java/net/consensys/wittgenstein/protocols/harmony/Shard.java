package net.consensys.wittgenstein.protocols.harmony;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Shard {
    public final int shardId;
    public final Map<Integer, Long> stakeholders;
    public final List<Integer> nodes;
    public final int epochLeader;
    public static int BEACON_SHARD = 0;

    public Shard(int shardId, List<Integer> tokens) {
        this.shardId = shardId;
        this.epochLeader = tokens.get(0);
        this.stakeholders = tokens.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        this.nodes = new ArrayList<>(stakeholders.keySet());
    }

    public Map<Integer, Long> getStakeholders() {
        return stakeholders;
    }
}
