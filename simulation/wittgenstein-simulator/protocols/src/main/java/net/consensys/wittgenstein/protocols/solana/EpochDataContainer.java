package net.consensys.wittgenstein.protocols.solana;

import net.consensys.wittgenstein.core.NodeBuilder;

import java.util.*;

public class EpochDataContainer<T> {

    private int currentEpoch = 0;
    private int previousEpoch = 1; // or next (depend on context of usage)
    private final List<T> container = new ArrayList<>(2);

    public EpochDataContainer(T previous, T current) {
        container.add(current);
        container.add(previous);
    }

    public void swap() {
        currentEpoch = (currentEpoch + 1) % 2;
        previousEpoch = (previousEpoch + 1) % 2;
    }

    public T getCurrent() {
        return this.container.get(currentEpoch);
    }

    public T getPrevious() {
        return this.container.get(previousEpoch);
    }

    public T getNext() {
        return this.container.get(previousEpoch);
    }
}
