package net.consensys.wittgenstein.protocols.harmony;

import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.core.Node;
import net.consensys.wittgenstein.core.NodeBuilder;
import net.consensys.wittgenstein.core.Protocol;
import org.apache.commons.cli.ParseException;

import java.util.Random;

public class Harmony implements Protocol {

    private final Network<HarmonyNode> network = new Network<>();
    private final int networkSize;

    public Harmony(int networkSize) {
        this.networkSize = networkSize;
    }

    @Override
    public Network<HarmonyNode> network() {
        return network;
    }

    @Override
    public Protocol copy() {
        return null;
    }

    @Override
    public void init() {

    }


    /** HarmonyNode */
    public class HarmonyNode extends Node {

        public HarmonyNode(Random rd, NodeBuilder nb) {
            super(rd, nb);
        }
    }
    /** HarmonyNode */

    public static void main(String[] args) throws ParseException {

    }

}
