package net.consensys.wittgenstein.protocols.harmony;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HarmonyConfig {
    public int epochDurationInSlots;
    public int numberOfEpochs;
    public int lastSlot;
    public int vdfInSlots;
    public int txSizeInBytes;
    public int blockHeaderSizeInBytes;
    public int networkSize;
    public int numberOfShards;
    public int expectedTxPerBlock;
    public int byzantineNodes;
    public int lambda;
    public boolean ddosAttacks;

    public boolean getDdosAttacks() {
        return ddosAttacks;
    }

    public void setDdosAttacks(boolean ddosAttacks) {
        this.ddosAttacks = ddosAttacks;
    }

    public int getLambda() {
        return lambda;
    }

    public void setLambda(int lambda) {
        this.lambda = lambda;
    }

    public int getEpochDurationInSlots() {
        return epochDurationInSlots;
    }

    public void setEpochDurationInSlots(int epochDurationInSlots) {
        this.epochDurationInSlots = epochDurationInSlots;
    }

    public int getNumberOfEpochs() {
        return numberOfEpochs;
    }

    public void setNumberOfEpochs(int numberOfEpochs) {
        this.numberOfEpochs = numberOfEpochs;
    }

    public int getLastSlot() {
        return lastSlot;
    }

    public void setLastSlot(int lastSlot) {
        this.lastSlot = lastSlot;
    }

    public int getVdfInSlots() {
        return vdfInSlots;
    }

    public void setVdfInSlots(int vdfInSlots) {
        this.vdfInSlots = vdfInSlots;
    }

    public int getTxSizeInBytes() {
        return txSizeInBytes;
    }

    public void setTxSizeInBytes(int txSizeInBytes) {
        this.txSizeInBytes = txSizeInBytes;
    }

    public int getBlockHeaderSizeInBytes() {
        return blockHeaderSizeInBytes;
    }

    public void setBlockHeaderSizeInBytes(int blockHeaderSizeInBytes) {
        this.blockHeaderSizeInBytes = blockHeaderSizeInBytes;
    }

    public int getNetworkSize() {
        return networkSize;
    }

    public void setNetworkSize(int networkSize) {
        this.networkSize = networkSize;
    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public int getExpectedTxPerBlock() {
        return expectedTxPerBlock;
    }

    public void setExpectedTxPerBlock(int expectedTxPerBlock) {
        this.expectedTxPerBlock = expectedTxPerBlock;
    }

    public int getByzantineNodes() {
        return byzantineNodes;
    }

    public void setByzantineNodes(int byzantineNodes) {
        this.byzantineNodes = byzantineNodes;
    }
}
