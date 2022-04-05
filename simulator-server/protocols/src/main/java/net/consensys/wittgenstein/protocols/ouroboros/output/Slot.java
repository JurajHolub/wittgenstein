package net.consensys.wittgenstein.protocols.ouroboros.output;

import net.consensys.wittgenstein.protocols.ouroboros.Block;

public class Slot {
    public int node;
    public int transactions;
    public int headerSize;
    public int txSize;
    public int epoch;
    public int slot;
    public long createTime;
    public long receiveTime;
    public long msgReceived;
    public long msgSent;
    public long bytesReceived;
    public long bytesSent;
    public int hopCount;

    public Slot(
            Block block,
            int nodeId,
            long time,
            long msgReceived,
            long msgSent,
            long bytesReceived,
            long bytesSent,
            int hopCount
    ) {
        this.node = nodeId;
        this.transactions = block.transactions;
        this.headerSize = block.headerSize;
        this.txSize = block.txSize;
        this.epoch = block.epoch;
        this.slot = block.slot;
        this.createTime = block.createTime;
        this.receiveTime = time;
        this.msgReceived = msgReceived;
        this.msgSent = msgSent;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.hopCount = hopCount;
    }
}
