package net.consensys.wittgenstein.protocols.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ByzantineShare {

    public static List<Integer> prepareByzantineNodes(double byzantineShare, List<Integer> nodesStake) {
        double eps = byzantineShare / 100;
        double totalStake = nodesStake.stream().mapToDouble(i->i).sum();
        List<Double> nodesProbability = nodesStake.stream().mapToDouble(s -> s / totalStake).boxed().collect(Collectors.toList());

        List<Integer> byzantineNodes = new ArrayList<>();

        double accum = 0d;
        for (int node = 0; node < nodesProbability.size(); node++) {
            double nodeStake = nodesProbability.get(node);

            if (Math.abs(byzantineShare - (accum + nodeStake)) <= eps) break;

            accum += nodeStake;
            byzantineNodes.add(node);
        }

        return byzantineNodes;
    }
}
