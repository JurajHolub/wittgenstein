package net.consensys.wittgenstein.protocols.harmony.rbs;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.protocols.harmony.Block;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;
import net.consensys.wittgenstein.protocols.harmony.Shard;
import net.consensys.wittgenstein.protocols.harmony.StakeDistribution;
import net.consensys.wittgenstein.protocols.harmony.rbs.Protocol.LeaderInit;
import net.consensys.wittgenstein.protocols.harmony.rbs.Protocol.PseudoRand;
import net.consensys.wittgenstein.protocols.harmony.rbs.Protocol.ValidatorInit;

import java.util.List;
import java.util.Random;

/**
 * Randomness Based Sharding used in Harmony for leader schedule.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class RandomnessBasedSharding {
    protected final transient Network<HarmonyNode> network;
    protected final transient HarmonyNode me;
    protected final transient StakeDistribution stakeDistribution;
    public RandomNumberCollector randomNumberCollector;
    public Integer pRand = null;
    public Integer rnd = null;

    public RandomnessBasedSharding(Random rd, NodeBuilder nb, Network<HarmonyNode> network, HarmonyNode me, StakeDistribution stakeDistribution) {
        this.network = network;
        this.me = me;
        this.stakeDistribution = stakeDistribution;
        this.randomNumberCollector = new RandomNumberCollector();
    }

    /**
     * 1. The leader sends a hash of the last block to all validators.
     */
    public void onDRG(Block block, List<HarmonyNode> validators) {
        int lastBlockHash = block.hashCode(); // hash of last block (this is enough for simulation)
        network.send(new LeaderInit(lastBlockHash), me, validators);
    }

    /**
     * 2. Each validator calculates a random number from the received hash using VRF, which it sends back to the leader.
     */
    public void onLeaderInit(HarmonyNode leader, int lastBlockHash) {
        int randomNumber = network.rd.nextInt() ^ lastBlockHash; // simulation of VRF
        network.send(new ValidatorInit(randomNumber), me, leader);
    }

    /**
     * 3. When the leader receives 1/3 of the random numbers, he makes an XOR above them. It inserts the resulting
     * pRand value into the new block using the FBFT consensus.
     */
    public void onValidatorInit(HarmonyNode validator, int randomNumber) {
        randomNumberCollector.add(randomNumber);

        int shardSize = stakeDistribution.getBeaconShard().nodes.size();
        if (!randomNumberCollector.containsThirdOfValues(shardSize)) return;
        if (randomNumberCollector.sent) return;
        randomNumberCollector.sent = true;

        int pRand = randomNumberCollector.xor();
        PseudoRand pseudoRand = new PseudoRand(pRand);
        network.send(pseudoRand, me, me);
    }

    /**
     * 4. When pRand is confirmed in the block, the leader calculates the final random number rnd from this value
     * using VDF. VDF guarantees that the calculation of rnd will take so long to produce a specific number of new
     * blocks. When the leader calculates the rnd, he inserts it into a new block using FBFT.
     */
    public void onPRandCommit(int rnd) {
        this.rnd = rnd;
    }

}
