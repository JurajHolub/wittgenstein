package net.consensys.wittgenstein.protocols.solana;

public class SlotData {
    public int txCounterVote;
    public int receivedVotingPower;
    public int totalVotingPower;
    public int txCounterNonVote;
    public int arriveTime;

    SlotData() {
        this.txCounterNonVote = 0;
        this.arriveTime = 0;
        this.receivedVotingPower = 0;
        this.totalVotingPower = 0;
        this.txCounterVote = 0;
    }

    SlotData(int txCounterNonVote, int receivedVotingPower, int totalVotingPower, int txCounterVote, int arriveTime) {
        this.txCounterNonVote = txCounterNonVote;
        this.arriveTime = arriveTime;
        this.receivedVotingPower = receivedVotingPower;
        this.totalVotingPower = totalVotingPower;
        this.txCounterVote = txCounterVote;
    }

    public void reset() {
        this.receivedVotingPower = 0;
        this.totalVotingPower = 0;
        this.txCounterVote = 0;
    }
}

