package net.consensys.wittgenstein.protocols.solana;

public class Block {
    public int slot;
    public int epoch;
    public int txCounterVote;
    public int receivedVotingPower;
    public int txCounterNonVote;
    public int createTime;
    public int txSizeInBytes;
    public int voteSizeInBytes;
    public int headerSizeInBytes = 80;

    public Block(
        int slot,
        int epoch,
        int txCounterVote,
        int receivedVotingPower,
        int txCounterNonVote,
        int createTime,
        int txSizeInBytes,
        int voteSizeInBytes
    ) {
        this.slot = slot;
        this.epoch = epoch;
        this.txCounterVote = txCounterVote;
        this.receivedVotingPower = receivedVotingPower;
        this.txCounterNonVote = txCounterNonVote;
        this.createTime = createTime;
        this.txSizeInBytes = txSizeInBytes;
        this.voteSizeInBytes = voteSizeInBytes;
    }

    public Block(int slot, int epoch) {
        this(slot, epoch, 0,0,0,0,0,0);
    }

    public void reset() {
        this.receivedVotingPower = 0;
        this.txCounterVote = 0;
    }

    public int size() {
        return txCounterNonVote * txSizeInBytes + txCounterVote * voteSizeInBytes + headerSizeInBytes;
    }

    /**
     * There are 2/3 BFT consensus.
     */
    public boolean isValid(int totalVotingPower) {
        return receivedVotingPower >= (2f/3f) * totalVotingPower;
    }
}

