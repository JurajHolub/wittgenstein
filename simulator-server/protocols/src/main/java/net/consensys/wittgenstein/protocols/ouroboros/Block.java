package net.consensys.wittgenstein.protocols.ouroboros;

public class Block {

    public int transactions;
    public int headerSize;
    public int txSize;
    public int epoch;
    public int slot;
    public int creator;
    public long createTime;

    public Block() {
        this(-1, -1,-1,0,0,0, 0);
    }

    public Block(
            int creator,
            int slot,
            int epoch,
            int transactions,
            int headerSize,
            int txSize,
            long createTime
    ) {
        this.creator = creator;
        this.slot = slot;
        this.epoch = epoch;
        this.transactions = transactions;
        this.headerSize = headerSize;
        this.txSize = txSize;
        this.createTime = createTime;
    }

    public int size() {
        return headerSize + transactions * txSize;
    }
}
