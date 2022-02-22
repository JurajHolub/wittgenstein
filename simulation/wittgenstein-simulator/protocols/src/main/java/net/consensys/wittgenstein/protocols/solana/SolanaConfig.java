package net.consensys.wittgenstein.protocols.solana;

public class SolanaConfig {
    static int TICKS_PER_SECOND = 160; // 1tick = 1000ms / 160tick = 6.25ms
    static int SLOT_DURATION_IN_TICKS = 64; // 64ticks
    static int SLOT_DURATION_IN_MS = 400; // 64tick * 6.25ms = 400ms
    static int EPOCH_DURATION_IN_SLOTS = 4200; // 420_000
    static int LEADER_SCHEDULE_TRIGGER = EPOCH_DURATION_IN_SLOTS / 3 * 2;
    static int MIN_POOR_STAKE = 1;
    static int MAX_POOR_STAKE = 10;
    static int MIN_RICH_STAKE = 1000;
    static int MAX_RICH_STAKE = 5000;
    static int AVERAGE_STAKE_INCREASE_PER_EPOCH = 45;
    static int AVERAGE_STAKE_DECREASE_PER_EPOCH = 45;
    static int MAX_TRANSACTION_PER_SLOT = 350;

    public static class TxPerSlot {
        static int MIN = 756;
        static int MAX = 1801;
        static int MEAN = 1362;
        static int STD_DEVIATION = 322;
    }
    public static class VotesPerSlot {
        static int MIN = 527;
        static int MAX = 2010;
        static int MEAN = 1180;
        static int STD_DEVIATION = 368;
    }
    public static class NonVotesPerSlot {
        static int MIN = TxPerSlot.MIN - VotesPerSlot.MIN;
        static int MAX = TxPerSlot.MAX - VotesPerSlot.MAX;
        static int MEAN = TxPerSlot.MEAN - VotesPerSlot.MEAN;
        static int STD_DEVIATION = TxPerSlot.STD_DEVIATION - VotesPerSlot.STD_DEVIATION;
    }

    public static class NetworkSpeed {
        static int MAX_BANDWIDTH = 1_000_000_000; // 1 Gbps
        static int TRANSACTION_SIZE = 176*8; //
        static int MAX_TPS = MAX_BANDWIDTH / TRANSACTION_SIZE; // 710k
    }
}
