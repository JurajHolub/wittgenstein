package net.consensys.wittgenstein.protocols.harmony;

import java.util.BitSet;
import java.util.Map;
import java.util.stream.IntStream;

public class BlockSigners extends Block {
    public final BitSet signatures;
    public final int validators;
    public boolean sent = false;

    public BlockSigners(Block block, int validators) {
        super(block);
        this.signatures = new BitSet(validators);
        this.validators = validators;
    }

    public void sign(int validator) {
        signatures.set(validator);
    }

    public boolean majoritySigned() {
        return signatures.cardinality() >= validators * (2.0/3.0);
    }

    public IntStream getSignedValidators() {
        return signatures.stream();
    }

    public int size() {
        return headerSize;
    }

}
