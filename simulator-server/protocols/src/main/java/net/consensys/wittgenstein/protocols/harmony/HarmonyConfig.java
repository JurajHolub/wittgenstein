package net.consensys.wittgenstein.protocols.harmony;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.consensys.wittgenstein.protocols.utils.SharedConfig;

/**
 * Harmony simulation input parameters.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HarmonyConfig extends SharedConfig {
    /**
     * Slot that starts VDF distributed randomness generation.
     */
    public int vdfInSlots;
    /**
     * Number of shard in Harmony network.
     */
    public int numberOfShards;
    /**
     * Security parameter lambda (recommended value is 600).
     */
    public int lambda;
    /**
     * If true, then perform graduating DoS attack.
     */
    public boolean ddosAttacks = false;
    /**
     * If ddosAttacks=True and vrfLeaderSelection´True then it determines number of nodes (richest) in every shard,
     * which will be under DoS attack.
     */
    public int shardDoSMax = 0;
}
