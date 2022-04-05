package net.consensys.wittgenstein.protocols.solana;

import net.consensys.wittgenstein.protocols.utils.SharedConfig;

/**
 * Input parameters specirfic for Solana protocol simulation.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class SolanaConfig extends SharedConfig {

    /**
     * Slot that triggers leader schedule recalculation.
     */
    public int leaderScheduleTrigger = epochDurationInSlots / 3 * 2;
    /**
     * Reliability of node in range <0,1> that represent probability of not failure (1 means absolutely realiable node).
     */
    public double validatorReliability = 1.0;
    /**
     * Number of leader nodes under DoS attack.
     */
    public int numberOfNodesUnderAttack;
    /**
     * Size of RSA digital signature [bytes].
     */
    public static int RSA_SIGNATURE_SIZE_IN_BYTES = 256;

}
