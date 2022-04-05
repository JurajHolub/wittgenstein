package net.consensys.wittgenstein.protocols.utils;

import java.util.*;
import java.util.stream.Collectors;

public class SortMapDescending {

    /**
     * @param counts Map of histogram.
     * @return Sorted histogram map (descending order).
     */
    public static LinkedHashMap sort(Map<Integer, Long> counts) {
        LinkedHashMap<Integer, Long> reverseSortedMap = new LinkedHashMap<>();
        counts.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));

        return reverseSortedMap;
    }
}
