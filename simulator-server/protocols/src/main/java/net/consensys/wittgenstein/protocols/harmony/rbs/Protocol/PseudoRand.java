package net.consensys.wittgenstein.protocols.harmony.rbs.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;

public class PseudoRand  extends Message<HarmonyNode> {
    private final int pRand;

    public PseudoRand(int pRand) {
        this.pRand = pRand;
    }


    @Override
    public void action(Network<HarmonyNode> network, HarmonyNode from, HarmonyNode to) {
        to.fbft.onPseudoRand(pRand);
    }
}
