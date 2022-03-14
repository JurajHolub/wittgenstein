package net.consensys.wittgenstein.protocols.harmony.output;

public class SlotStats{
    public int node;
    public int shard;
    public int slot;
    public int transactions;
    public int time;
    public long msgReceived;
    public long msgSent;
    public long bytesReceived;
    public long bytesSent;

    public SlotStats(int node, int shard, int slot, int transactions, int time, long msgReceived, long msgSent, long bytesReceived, long bytesSent) {
        this.node = node;
        this.shard = shard;
        this.slot = slot;
        this.transactions = transactions;
        this.time = time;
        this.msgReceived = msgReceived;
        this.msgSent = msgSent;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getShard() {
        return shard;
    }

    public void setShard(int shard) {
        this.shard = shard;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getTransactions() {
        return transactions;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public long getMsgReceived() {
        return msgReceived;
    }

    public void setMsgReceived(long msgReceived) {
        this.msgReceived = msgReceived;
    }

    public long getMsgSent() {
        return msgSent;
    }

    public void setMsgSent(long msgSent) {
        this.msgSent = msgSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }
}
