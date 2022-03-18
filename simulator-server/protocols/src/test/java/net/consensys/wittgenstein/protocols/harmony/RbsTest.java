package net.consensys.wittgenstein.protocols.harmony;

import junit.framework.TestCase;
import net.consensys.wittgenstein.core.NetworkLatency;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;

public class RbsTest extends TestCase {
    private Harmony harmony;
    int networkSize;
    int numberOfShards;

    @Override
    protected void setUp() throws Exception {
        networkSize = 200;
        numberOfShards = 4;
        harmony = new Harmony(networkSize, numberOfShards, 500, 500, 0);
        harmony.network().networkLatency = new NetworkLatency.NetworkFixedLatency(1);
        harmony.init();
    }

    public void testRbs() {
        int epochs = 3;
        harmony.simulate(3);
        //TODO
    }
}
