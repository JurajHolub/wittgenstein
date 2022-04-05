package net.consensys.wittgenstein.protocols.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility helps to choose nodes for DoS attack.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class DosAttackUtil {

    /**
     * Use this in case of protocol with leader schedule for epoch.
     * @return list of size numberOfNodesUnderAttack with most frequent leaders
     */
    public List<Integer> bestLeadersToAttackWithSchedule(List<Integer> leaderSchedule, int numberOfNodesUnderAttack) {
        Map<Integer, Long> histogram = leaderSchedule.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        LinkedHashMap<Integer, Long> reverseSortedMap = new LinkedHashMap<>();
        histogram.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        List<Integer> mostFrequentLeaders = new ArrayList<>(reverseSortedMap.keySet()).stream()
                .limit(numberOfNodesUnderAttack)
                .collect(Collectors.toList());

        return mostFrequentLeaders;
    }

    /**
     * Use this in case of protocol with unpredictable VRF leader selection.
     * @return list of size numberOfNodesUnderAttack with the richest nodes
     */
    public List<Integer> bestLeadersToAttackWithVDF(List<Integer> stakeDistribution, int numberOfNodesUnderAttack) {
        Map<Integer, Long> stake = new HashMap<>();
        for (int node = 0; node < stakeDistribution.size(); node++) {
            stake.put(node, (long) stakeDistribution.get(node));
        }
        LinkedHashMap<Integer, Long> sorted = SortMapDescending.sort(stake);
        return sorted.keySet().stream().limit(numberOfNodesUnderAttack).collect(Collectors.toList());
    }
}
