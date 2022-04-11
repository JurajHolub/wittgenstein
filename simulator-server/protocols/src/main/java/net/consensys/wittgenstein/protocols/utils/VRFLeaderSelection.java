package net.consensys.wittgenstein.protocols.utils;

import java.util.*;

/**
 * VRF feature inspired by Algorand leader selection.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class VRFLeaderSelection {
    /**
     * Actual leaders for each slot.
     */
    public Map<Integer, Integer> slots;
    /** PNRG */
    private  AliasMethod aliasMethod;
    /**
     * Candidates for leader selection.
     */
    public final List<Integer> nodes;

    public VRFLeaderSelection(int epoch, List<Integer> nodes, List<Double> nodesStakeProbability) {
        this.slots = new HashMap<>();
        this.nodes = nodes;
        updateStake(epoch, nodesStakeProbability);
    }

    public void updateStake(int epoch, List<Double> nodesStakeProbability) {
        this.aliasMethod = new AliasMethod(nodesStakeProbability, new Random(epoch + 0x1234));
        this.slots.clear();
    }

    /**
     * Simulate Algorand unpredictable leader selection. First call with given slot select leader.
     * All other calls with the same slot returns same leader.
     */
    public int chooseSlotLeader(int slot) {
        slots.computeIfAbsent(slot, s -> aliasMethod.next());
        return slots.get(slot);
    }
}
