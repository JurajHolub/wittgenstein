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
     * 1. Vodca pošle hash posledného bloku všetkým validátorom.
     */
    public void onDRG(Block block, List<HarmonyNode> validators) {
        int lastBlockHash = block.hashCode(); // hash of last block (this is enough for simulation)
        network.send(new LeaderInit(lastBlockHash), me, validators);
    }

    /**
     * 2. Každý validátor vypočíta s prijatého hashu pomocou VRF náhodné číslo, ktoré pošle späť vodcovi.
     */
    public void onLeaderInit(HarmonyNode leader, int lastBlockHash) {
        int randomNumber = network.rd.nextInt() ^ lastBlockHash; // simulation of VRF
        network.send(new ValidatorInit(randomNumber), me, leader);
    }

    /**
     * 3. Keď vodca príjme 1/3 náhodných čísel, tak nad nimi urobí XOR. Výslednú hodnotu pRand vloží do nového bloku
     * pomocou FBFT konsenzu.
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
     * 4. Keď je pRand potvrdená v bloku, vypočíta vodca z tejto hodnoty finálne náhodne číslo rnd pomocou VDF.
     * VDF garantuje, že výpočet rnd zaberie toľko času aby sa stihlo vyprodukovať špecifické množstvo nových blokov.
     * Keď vodca vypočíta rnd tak ho pomocou FBFT vloží do nového bloku.
     */
    public void onPRandCommit(int rnd) {
        this.rnd = rnd;
    }

}
