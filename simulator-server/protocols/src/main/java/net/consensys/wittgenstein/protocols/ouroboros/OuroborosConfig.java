package net.consensys.wittgenstein.protocols.ouroboros;

import net.consensys.wittgenstein.protocols.utils.SharedConfig;

/**
 * Input parameters of simulation specific for Ourobors simulation.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class OuroborosConfig extends SharedConfig {
    public int p2pConnectionCount = 20;
    public boolean p2pMinimum = false;
    public int numberOfNodesUnderDos = 0;
}
