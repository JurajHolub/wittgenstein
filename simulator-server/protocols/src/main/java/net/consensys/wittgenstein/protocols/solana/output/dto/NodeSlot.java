package net.consensys.wittgenstein.protocols.solana.output.dto;

import net.consensys.wittgenstein.protocols.solana.Block;

public class NodeSlot {

    public int slot;
    public int epoch;
    public int node;
    public boolean isLeader;
    public int txCounterVote;
    public int receivedVotingPower;
    public int txCounterNonVote;

    public long createTime;
    public long receiveTime;
    public long msgReceived;
    public long msgSent;
    public long bytesReceived;
    public long bytesSent;

    public NodeSlot(
            int node,
            Block slotData,
            long receiveTime,
            long msgReceived,
            long msgSent,
            long bytesReceived,
            long bytesSent,
            boolean isLeader
    )
    {
        this.node = node;
        this.slot = slotData.slot;
        this.epoch = slotData.epoch;
        this.isLeader = isLeader;
        this.txCounterVote = slotData.txCounterVote;
        this.receivedVotingPower = slotData.receivedVotingPower;
        this.txCounterNonVote = slotData.txCounterNonVote;
        this.createTime = slotData.createTime;
        this.receiveTime = receiveTime;

        this.msgReceived = msgReceived;
        this.msgSent = msgSent;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
    }
}
