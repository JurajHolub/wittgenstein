package net.consensys.wittgenstein.protocols.utils;

public class SharedConfig {
    /**
     * Size of simulate network (number of nodes).
     */
    public int networkSize;
    /**
     * Number of slots in every epoch.
     */
    public int epochDurationInSlots;
    /**
     * Number of epoch to simulate.
     */
    public int numberOfEpochs;
    /**
     * Duration of every slot [ms].
     */
    public int slotDurationInMs;
    /**
     * Size of average application transaction [Bytes].
     */
    public int txSizeInBytes;
    /**
     * Size of a single block header in blockchain [Bytes].
     */
    public int blockHeaderSizeInBytes;
    /**
     * Number of transaction that usually generates application layer for a single block.
     */
    public int expectedTxPerBlock;
    /**
     * Proposed feature that replaces leader schedule.
     */
    public boolean vrfLeaderSelection = false;
    /**
     * Addres to mongoDB. In case of release docker it is 'mongodb'. In case of debug 'localhost:27017'.
     */
    public String mongoServerAddress;
    /**
     * If true then all nodes have stake from uniform distribution.
     * Otherwise, stake is taken from configuration files in resources:
     * CardanoStake--2022-04-04.csv, HarmonyStake-2022-02-24.csv, SolanaStake-2022-02-22.csv
     */
    public boolean uniformStakeDistribution = false;
}
