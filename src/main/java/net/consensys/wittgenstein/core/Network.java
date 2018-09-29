package net.consensys.wittgenstein.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * There is a single network for a simulation.
 * <p>
 * Nothing is executed in parallel, so the code does not have to be multithread safe.
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Network<TN extends Node> {
    final static int duration = 60 * 1000;

    /**
     * The messages in transit. Sorted by their arrival time.
     */
    public final MessageStorage msgs = new MessageStorage();

    /**
     * In parallel of the messages, we have tasks. It's mixed with messages (some tasks
     * are managed as special messages). Conditional tasks are in a specific list.
     */
    public final List<ConditionalTask> conditionalTasks = new LinkedList<>();


    /**
     * Internal variable. Nodes id are sequential & start at zero, so we can we index them in an array.
     */
    protected final List<TN> allNodes = new ArrayList<>(2048);

    /**
     * By using a single random generator, we have repeatable runs.
     */
    public final Random rd;

    final List<Integer> partitionsInX = new ArrayList<>();

    /**
     * We can decide to discard messages that would take too long to arrive. This
     * limit the memory consumption of the simulator as well.
     */
    int msgDiscardTime = Integer.MAX_VALUE;

    /**
     * The network latency. The default one is quite pessimistic, you
     * may want to use another one.
     */
    NetworkLatency networkLatency = new NetworkLatency.EthScanNetworkLatency();

    /**
     * Time in ms. Using an int limits us to ~555 hours of simulation, it's acceptable, and
     * as we have billions of dated objects it saves some memory...
     */
    public int time = 0;

    public Network() {
        this(0);
    }

    public Network(long randomSeed) {
        this.rd = new Random(randomSeed);
    }

    public TN getNodeById(int id) {
        return allNodes.get(id);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Network<TN> setMsgDiscardTime(int l) {
        this.msgDiscardTime = l;
        return this;
    }


    /**
     * A desperate attempt to have something less memory consuming than a PriorityQueue or
     * a guava multimap, by using raw array. The idea is to optimize the case when there are
     * multiple messages in the same millisecond, but the global time range for all messages is limited.
     * <p>
     * The second idea is to have a repeatable run when there are multiple messages arriving
     * at the same millisecond.
     */
    private final class MsgsSlot {
        final int startTime;
        final int endTime;
        @SuppressWarnings("unchecked")
        final Message<?>[] msgsByMs = new Message[duration];

        public MsgsSlot(int startTime) {
            this.startTime = startTime - (startTime % duration);
            this.endTime = startTime + duration;
        }

        private int getPos(int aTime) {
            if (aTime < startTime || aTime >= startTime + duration) {
                throw new IllegalArgumentException();
            }
            return (aTime % duration);
        }

        public void addMsg(@NotNull Message<?> m) {
            int pos = getPos(m.nextArrivalTime(Network.this));
            m.setNextSameTime(msgsByMs[pos]);
            msgsByMs[pos] = m;
        }

        public @Nullable Message<?> peek(int time) {
            int pos = getPos(time);
            return msgsByMs[pos];
        }

        public @Nullable Message<?> poll(int time) {
            int pos = getPos(time);
            Message<?> m = msgsByMs[pos];
            if (m != null) {
                msgsByMs[pos] = m.getNextSameTime();
            }
            return m;
        }

        public int size() {
            int size = 0;
            for (int i = 0; i < duration; i++) {
                int ss = 0;
                Message m = msgsByMs[i];
                while (m != null) {
                    ss++;
                    m = m.getNextSameTime();
                }
                size += ss;
            }
            return size;
        }

        @Nullable Message<?> peekFirst() {
            for (int i = 0; i < duration; i++) {
                if (msgsByMs[i] != null) {
                    return msgsByMs[i];
                }
            }
            return null;
        }
    }

    public final class MessageStorage {
        public final ArrayList<MsgsSlot> msgsBySlot = new ArrayList<>();

        public int size() {
            int size = 0;
            for (MsgsSlot ms : msgsBySlot) {
                size += ms.size();
            }
            return size;
        }

        void cleanup() {
            while (!msgsBySlot.isEmpty() && time >= msgsBySlot.get(0).endTime) {
                msgsBySlot.remove(0);
            }
            if (msgsBySlot.isEmpty()) {
                msgsBySlot.add(new MsgsSlot(time));
            }
        }

        void ensureSize(int aTime) {
            while (msgsBySlot.get(msgsBySlot.size() - 1).endTime <= aTime) {
                msgsBySlot.add(new MsgsSlot(msgsBySlot.get(msgsBySlot.size() - 1).endTime));
            }
        }

        @NotNull MsgsSlot findSlot(int aTime) {
            cleanup();
            ensureSize(aTime);
            int pos = (aTime - msgsBySlot.get(0).startTime) / duration;
            return msgsBySlot.get(pos);
        }

        void addMsg(@NotNull Message<?> m) {
            findSlot(m.nextArrivalTime(Network.this)).addMsg(m);
        }

        @Nullable Message<?> peek(int time) {
            return findSlot(time).peek(time);
        }

        @Nullable Message<?> poll(int time) {
            return findSlot(time).poll(time);
        }

        public void clear() {
            msgsBySlot.clear();
            cleanup();
        }

        /**
         * @return the first message in the queue, null if the queue is empty.
         */
        @Nullable Message<?> peekFirst() {
            for (MsgsSlot ms : msgsBySlot) {
                Message<?> m = ms.peekFirst();
                if (m != null) return m;
            }
            return null;
        }


        /**
         * For tests: they can find their message content.
         */
        public @Nullable MessageContent<?> peekFirstMessageContent() {
            Message<?> m = peekFirst();
            return m == null ? null : m.getMessageContent();
        }

        /**
         * @return the first message in the queue, null if the queue is empty.
         */
        public @Nullable Message<?> pollFirst() {
            Message<?> m = peekFirst();
            return m == null ? null : poll(m.nextArrivalTime(Network.this));
        }
    }


    /**
     * The generic message that goes on a network. Triggers an 'action' on reception.
     * <p>
     * Object of this class must be immutable. Especially, MessageContent is shared between
     * the messages for messages sent to multiple nodes.
     */
    public static abstract class MessageContent<TN extends Node> {
        public abstract void action(@NotNull TN from, @NotNull TN to);

        /**
         * We track the total size of the messages exchanged. Subclasses should
         * override this method if they want to measure the network usage.
         */
        public int size() {
            return 1;
        }
    }

    /**
     * Some protocols want some tasks to be executed at a given time
     */
    public class Task extends MessageContent<TN> {
        final @NotNull Runnable r;

        public Task(@NotNull Runnable r) {
            this.r = r;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void action(@NotNull TN from, @NotNull TN to) {
            r.run();
        }
    }

    public final class ConditionalTask extends Task {
        /**
         * Starts if this condition is met.
         */
        final Condition startIf;

        /**
         * Will start again when the task is finished if this condition is met.
         */
        final Condition repeatIf;

        /**
         * Time before next start.
         */
        final int duration;
        final Node sender;

        /**
         * Will start after this time.
         */
        int minStartTime;

        public ConditionalTask(Condition startIf, Condition repeatIf, Runnable r, int minStartTime, int duration, Node sender) {
            super(r);
            this.startIf = startIf;
            this.repeatIf = repeatIf;
            this.duration = duration;
            this.sender = sender;
            this.minStartTime = minStartTime;
        }
    }


    public interface Condition {
        boolean check();
    }

    /**
     * Some protocols want some tasks to be executed periodically
     */
    public class PeriodicTask extends Task {
        final int period;
        final Node sender;
        final Condition continuationCondition;

        public PeriodicTask(@NotNull Runnable r, @NotNull Node fromNode, int period, @NotNull Condition condition) {
            super(r);
            this.period = period;
            this.sender = fromNode;
            this.continuationCondition = condition;
        }

        public PeriodicTask(@NotNull Runnable r, @NotNull Node fromNode, int period) {
            this(r, fromNode, period, () -> true);
        }

        @Override
        public void action(@NotNull Node from, @NotNull Node to) {
            r.run();
            if (continuationCondition.check()) {
                msgs.addMsg(new Message.SingleDestMessage<>(this, sender, sender, time + period));
            }
        }
    }


    /**
     * Simulate for x seconds. Can be called multiple time.
     */
    public void run(int seconds) {
        runMs(seconds * 1000);
    }

    public void runMs(int ms) {
        int endAt = time + ms;
        receiveUntil(endAt);
        time = endAt;
    }


    /**
     * Send a message to all nodes.
     */
    public void sendAll(@NotNull MessageContent<? extends TN> m, int sendTime, @NotNull TN fromNode) {
        send(m, sendTime, fromNode, allNodes);
    }

    public void sendAll(@NotNull MessageContent<? extends TN> m, @NotNull TN fromNode) {
        send(m, time + 1, fromNode, allNodes);
    }

    /**
     * Send a message to a collection of nodes. The message is considered as sent immediately, and
     * will arrive at a time depending on the network latency.
     */
    public void send(@NotNull MessageContent<? extends TN> m, @NotNull TN fromNode, @NotNull Collection<TN> dests) {
        send(m, time + 1, fromNode, dests);
    }

    public void send(@NotNull MessageContent<? extends TN> m, @NotNull TN fromNode, @NotNull TN toNode) {
        send(m, time + 1, fromNode, toNode);
    }

    /**
     * Send a message to a single node.
     */
    public void send(@NotNull MessageContent<? extends TN> mc, int sendTime, @NotNull TN fromNode, @NotNull TN toNode) {
        MessageArrival ms = createMessageArrival(mc, fromNode, toNode, sendTime, rd.nextInt());
        if (ms != null) {
            Message<?> m = new Message.SingleDestMessage<>(mc, fromNode, toNode, ms.arrival);
            msgs.addMsg(m);
        }
    }

    final static class MessageArrival implements Comparable<MessageArrival> {
        final Node dest;
        final int arrival;

        public MessageArrival(@NotNull Node dest, int arrival) {
            this.dest = dest;
            this.arrival = arrival;
        }

        @Override
        public int compareTo(@NotNull Network.MessageArrival o) {
            return Long.compare(arrival, o.arrival);
        }
    }

    public void send(@NotNull MessageContent<? extends TN> m, int sendTime, @NotNull TN fromNode, @NotNull Collection<? extends Node> dests) {
        int randomSeed = rd.nextInt();

        List<MessageArrival> da = createMessageArrivals(m, sendTime, fromNode, dests, randomSeed);

        if (!da.isEmpty()) {
            if (da.size() == 1) {
                MessageArrival ms = da.get(0);
                Message<?> msg = new Message.SingleDestMessage<>(m, fromNode, ms.dest, ms.arrival);
                msgs.addMsg(msg);
            } else {
                Message<?> msg = new Message.MultipleDestMessage<>(m, fromNode, da, sendTime, randomSeed);
                msgs.addMsg(msg);
            }
        }
    }

    @NotNull List<MessageArrival> createMessageArrivals(@NotNull MessageContent<? extends TN> m, int sendTime, @NotNull TN fromNode, @NotNull Collection<? extends Node> dests, int randomSeed) {
        ArrayList<MessageArrival> da = new ArrayList<>(dests.size());
        for (Node n : dests) {
            MessageArrival ma = createMessageArrival(m, fromNode, n, sendTime, randomSeed);
            if (ma != null) {
                da.add(ma);
            }
        }
        Collections.sort(da);

        return da;
    }


    private @Nullable MessageArrival createMessageArrival(@NotNull MessageContent<?> m,
                                                          @NotNull Node fromNode, @NotNull Node toNode, int sendTime,
                                                          int randomSeed) {
        if (sendTime <= time) {
            throw new IllegalStateException("" + m + ", sendTime=" + sendTime + ", time=" + time);
        }

        if (partitionId(fromNode) == partitionId(toNode)) {
            assert !(m instanceof Network.Task);
            fromNode.msgSent++;
            fromNode.bytesSent += m.size();
            int nt = networkLatency.getLatency(fromNode, toNode, getPseudoRandom(toNode.nodeId, randomSeed));
            if (nt < msgDiscardTime) {
                return new MessageArrival(toNode, sendTime + nt);
            }
        }

        return null;
    }

    /**
     * @return always the same number, between 0 and 99, uniformly distributed.
     */
    public static int getPseudoRandom(int nodeId, int randomSeed) {
        int x = hash(nodeId) ^ randomSeed;
        return Math.abs(x % 100);
    }

    private static int hash(int a) {
        a ^= (a << 13);
        a ^= (a >>> 17);
        a ^= (a << 5);
        return a;
    }

    public void registerTask(@NotNull final Runnable task, int startAt, @NotNull TN fromNode) {
        Task sw = new Task(task);
        msgs.addMsg(new Message.SingleDestMessage<>(sw, fromNode, fromNode, startAt));
    }

    public void registerPeriodicTask(@NotNull final Runnable task, int startAt, int period, @NotNull TN fromNode) {
        PeriodicTask sw = new PeriodicTask(task, fromNode, period);
        msgs.addMsg(new Message.SingleDestMessage<>(sw, fromNode, fromNode, startAt));
    }

    public void registerPeriodicTask(@NotNull final Runnable task, int startAt, int period, @NotNull TN fromNode, @NotNull Condition c) {
        PeriodicTask sw = new PeriodicTask(task, fromNode, period, c);
        msgs.addMsg(new Message.SingleDestMessage<>(sw, fromNode, fromNode, startAt));
    }

    public void registerConditionalTask(@NotNull final Runnable task, int startAt, int duration,
                                        @NotNull TN fromNode, @NotNull Condition startIf, @NotNull Condition repeatIf) {
        ConditionalTask ct = new ConditionalTask(startIf, repeatIf, task, startAt, duration, fromNode);
        conditionalTasks.add(ct);
    }

    private @Nullable Message<?> nextMessage(int until) {
        while (time <= until) {
            Message<?> m = msgs.poll(time);
            if (m != null) {
                return m;
            } else {
                time++;
            }
        }
        return null;
    }

    void receiveUntil(int until) {
        //noinspection ConstantConditions
        int previousTime = time;
        Message<?> next = nextMessage(until);
        while (next != null) {
            Message<?> m = next;
            if (m.nextArrivalTime(this) != previousTime) {
                if (time > m.nextArrivalTime(this)) {
                    throw new IllegalStateException("time:" + time + ", arrival=" + m.nextArrivalTime(this) + ", m:" + m);
                }

                Iterator<ConditionalTask> it = conditionalTasks.iterator();
                while (it.hasNext()) {
                    ConditionalTask ct = it.next();
                    if (!ct.repeatIf.check()) {
                        it.remove();
                    } else {
                        if (time >= ct.minStartTime && ct.startIf.check()) {
                            ct.r.run();
                            ct.minStartTime = time + ct.duration;
                        }
                    }
                }
            }

            TN from = allNodes.get(m.getFromId());
            TN to = allNodes.get(m.getNextDestId());
            if (partitionId(from) == partitionId(to)) {
                if (!(m.getMessageContent() instanceof Network<?>.Task)) {
                    if (m.getMessageContent().size() == 0) throw new IllegalStateException();
                    to.msgReceived++;
                    to.bytesReceived += m.getMessageContent().size();
                }
                @SuppressWarnings("unchecked")
                MessageContent<TN> mc = (MessageContent<TN>) m.getMessageContent();
                mc.action(from, to);
            }

            m.markRead();
            if (m.hasNextReader()) {
                msgs.addMsg(m);
            }
            previousTime = time;
            next = nextMessage(until);
        }
    }

    int partitionId(@NotNull Node to) {
        int pId = 0;
        for (Integer x : partitionsInX) {
            if (x > to.x) {
                return pId;
            } else {
                pId++;
            }
        }
        return pId;
    }


    public void addNode(@NotNull TN node) {
        while (allNodes.size() <= node.nodeId) {
            allNodes.add(null);
        }
        allNodes.set(node.nodeId, node);
    }


    /**
     * Set the network latency to a min value. This allows
     * to test the protocol independently of the network variability.
     */
    public @NotNull Network<TN> removeNetworkLatency() {
        networkLatency = new NetworkLatency.NetworkNoLatency();
        return setNetworkLatency(new NetworkLatency.NetworkNoLatency());
    }

    public @NotNull Network<TN> setNetworkLatency(int[] distribProp, int[] distribVal) {
        return setNetworkLatency(new NetworkLatency.MeasuredNetworkLatency(distribProp, distribVal));
    }

    public @NotNull Network<TN> setNetworkLatency(@NotNull NetworkLatency networkLatency) {
        if (msgs.size() != 0) {
            throw new IllegalStateException("You can't change the latency while the system as on going messages");
        }

        this.networkLatency = networkLatency;
        return this;
    }

    public void printNetworkLatency() {
        System.out.println("" + networkLatency);
    }

    /**
     * It's possible to cut the network in multiple points.
     * The partition takes the node positions into account
     * On a map, a partition is a vertical cut on the X axe (eg. the earth is not
     * round for these partitions)
     *
     * @param part - the x point (in %) where we cut.
     */
    public void partition(float part) {
        if (part <= 0 || part >= 1) {
            throw new IllegalArgumentException("part needs to be a percentage between 0 & 100 excluded");
        }
        int xPoint = (int) (Node.MAX_X * part);
        if (partitionsInX.contains(xPoint)) {
            throw new IllegalArgumentException("this partition exists already");
        }
        partitionsInX.add(xPoint);
        Collections.sort(partitionsInX);
    }

    public void endPartition() {
        partitionsInX.clear();
    }

}
