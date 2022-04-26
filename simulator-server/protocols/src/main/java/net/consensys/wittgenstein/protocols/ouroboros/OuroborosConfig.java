package net.consensys.wittgenstein.protocols.ouroboros;

import net.consensys.wittgenstein.protocols.utils.SharedConfig;

/**
 * Input parameters of simulation specific for Ourobors simulation.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class OuroborosConfig extends SharedConfig {
    /**
     * @see net.consensys.wittgenstein.core.P2PNetwork
     */
    public int p2pConnectionCount = 20;
    /**
     * @see net.consensys.wittgenstein.core.P2PNetwork
     */
    public boolean p2pMinimum = false;
    public int numberOfNodesUnderDos = 0;
    /**
     * Ratio of stake possesed by byzantine nodes, I range <0,1>. Simulation chosse
     * such a nodes that they stake sum is equal this value.
     */
    public double byzantineStake = 0.0;
    /**
     * If byzantineStake > 0 then it defines number of fork in case of byzantine leader.
     */
    public int forkRatio = 3;
}
