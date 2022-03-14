package net.consensys.wittgenstein.protocols.harmony;

import java.util.List;
import java.util.Objects;

public class Block {
    public final int shard;
    public final int epoch;
    public final int slot;
    public int time;
    public final int transactions;
    public Integer pRand = null;
    public Integer rnd = null;
    protected int headerSize;
    protected int txSize;

    /** Genesis block */
    public Block(int headerSize, int txSize) {
        this(
            0,
            0,
            0,
            0,
            null,
            null,
            headerSize,
            txSize
        );
    }

    public Block(int shard, int epoch, int slot, int transactions, int headerSize, int txSize) {
        this(
            shard,
            epoch,
            slot,
            transactions,
            null,
            null,
            headerSize,
            txSize
        );
    }

    public Block(int shard, int epoch, int slot, int transactions, Integer pRand, Integer rnd, int headerSize, int txSize) {
        this.shard = shard;
        this.epoch = epoch;
        this.slot = slot;
        this.transactions = transactions;
        this.pRand = pRand;
        this.rnd = rnd;
        this.headerSize = headerSize;
        this.txSize = txSize;
    }

    public Block(Block block) {
        this(
            block.shard,
            block.epoch,
            block.slot,
            block.transactions,
            block.pRand,
            block.rnd,
            block.headerSize,
            block.txSize
        );
    }

    public int getShard() {
        return shard;
    }

    public int getSlot() {
        return slot;
    }

    public int getEpoch() {
        return epoch;
    }

    public int getTransactions() {
        return transactions;
    }

    public Integer getPRand() {
        return pRand;
    }

    public Integer getRnd() {
        return rnd;
    }

    public boolean isHeaderValid() {
        return true;
    }

    public boolean isDataValid() {
        return true;
    }

    public boolean isLast(int epochDurationInSlots) {
        return slot+1 == epochDurationInSlots;
    }

    public boolean isBeaconShard() {
        return shard == Shard.BEACON_SHARD;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Slot) {
            Block s = (Block)obj;
            return Objects.equals(slot, s.slot) && Objects.equals(shard, s.shard);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return List.of(slot, shard).hashCode();
    }

    public int size() {
        return headerSize + txSize * transactions;
    }
}
