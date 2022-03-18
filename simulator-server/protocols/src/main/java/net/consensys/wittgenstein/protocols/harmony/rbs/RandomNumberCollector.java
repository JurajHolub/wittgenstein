package net.consensys.wittgenstein.protocols.harmony.rbs;

import java.util.ArrayList;
import java.util.List;

public class RandomNumberCollector {
    private final List<Integer> randomNumbers = new ArrayList<>();
    public boolean sent = false;

    public RandomNumberCollector() {
    }

    public void add(int randomNumber) {
        randomNumbers.add(randomNumber);
    }

    public int xor() {
        return randomNumbers.stream().reduce(0, (subtotal, element) -> subtotal ^ element);
    }

    public boolean containsThirdOfValues(int shardSize) {
        return randomNumbers.size() >= shardSize / 3.0;
    }

    public void clear() {
        randomNumbers.clear();
        sent = false;
    }
}
