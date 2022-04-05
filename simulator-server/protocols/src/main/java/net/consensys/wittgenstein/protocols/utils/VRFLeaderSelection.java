package net.consensys.wittgenstein.protocols.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * VRF feature inspired by Algorand leader selection.
 */
public class VRFLeaderSelection {
    /**
     * Actual leaders for each slot.
     */
    public List<Integer> slots;
    /** PNRG */
    private  AliasMethod aliasMethod;
    /**
     * Candidates for leader selection.
     */
    public final List<Integer> nodes;

    public VRFLeaderSelection(int epoch, List<Integer> nodes, List<Double> nodesStakeProbability) {
        this.slots = new ArrayList<>();
        this.nodes = nodes;
        updateStake(epoch, nodesStakeProbability);
    }

    public void updateStake(int epoch, List<Double> nodesStakeProbability) {
        this.aliasMethod = new AliasMethod(nodesStakeProbability, new Random(epoch + 1));
        this.slots.clear();
    }

    /**
     * Simulate Algorand unpredictable leader selection. First call with given slot select leader.
     * All other calls with the same slot returns same leader.
     */
    public int chooseSlotLeader(int slot) {
        if (slot == slots.size()) {
            slots.add(nodes.get(aliasMethod.next()));
        }

        return slots.get(slot);
    }
}
