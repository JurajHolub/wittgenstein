package net.consensys.wittgenstein.protocols.harmony.rbs.Protocol;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.messages.Message;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;

public class ValidatorInit extends Message<HarmonyNode> {
    private final int randomNumber;

    public ValidatorInit(int randomNumber) {
        this.randomNumber = randomNumber;
    }

    @Override
    public void action(Network<HarmonyNode> network, HarmonyNode from, HarmonyNode to) {
        to.rbs.onValidatorInit(from, randomNumber);
    }
}
