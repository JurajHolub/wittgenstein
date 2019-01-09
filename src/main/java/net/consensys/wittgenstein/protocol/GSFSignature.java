package net.consensys.wittgenstein.protocol;

import net.consensys.wittgenstein.core.*;
import net.consensys.wittgenstein.core.utils.MoreMath;
import net.consensys.wittgenstein.core.utils.StatsHelper;

import java.util.*;
import java.util.function.Predicate;

/**
 * A p2p protocol for BLS signature aggregation.
 * <p>
 * A node runs San Fermin and communicates the results a la gossip. So it's a Gossiping San Fermin
 * Runs in parallel a task to validate the signatures sets it has received.
 */
@SuppressWarnings("WeakerAccess") public class GSFSignature implements Protocol {
  /**
   * The number of nodes in the network
   */
  final int nodeCount;

  /**
   * The number of signatures to reach to finish the protocol.
   */
  final int threshold;

  /**
   * The time it takes to do a pairing for a node.
   */
  final int pairingTime;

  final public int timeoutPerLevelMs;
  final public int periodDurationMs;
  final int acceleratedCallsCount;

  final int nodesDown;

  final Network<GSFNode> network;
  Node.NodeBuilder nb;

  public GSFSignature(int nodeCount, int threshold, int pairingTime, int timeoutPerLevelMs,
    int periodDurationMs, int acceleratedCallsCount, int nodesDown, Node.NodeBuilder nb,
    NetworkLatency nl) {

    if (nodesDown >= nodeCount || nodesDown < 0 || threshold > nodeCount || (nodesDown + threshold
      > nodeCount)) {
      throw new IllegalArgumentException("nodeCount=" + nodeCount + ", threshold=" + threshold);
    }

    this.nodeCount = nodeCount;
    this.threshold = threshold;
    this.pairingTime = pairingTime;
    this.timeoutPerLevelMs = timeoutPerLevelMs;
    this.periodDurationMs = periodDurationMs;
    this.acceleratedCallsCount = acceleratedCallsCount;
    this.nodesDown = nodesDown;
    this.network = new Network<>();
    this.nb = nb;
    this.network.setNetworkLatency(nl);
  }

  public GSFSignature(int nodeCount, double ratioThreshold, int pairingTime, int timeoutPerLevelMs,
    int periodDurationMs, int acceleratedCallsCount, double ratioNodesDown) {
    this(nodeCount, (int) (ratioThreshold * nodeCount), pairingTime, timeoutPerLevelMs,
      periodDurationMs, acceleratedCallsCount, (int) (ratioNodesDown * nodeCount),
      new Node.NodeBuilderWithRandomPosition(), new NetworkLatency.IC3NetworkLatency());
  }

  @Override public String toString() {
    return "GSFSignature, " + "nodes=" + nodeCount + ", threshold=" + threshold + ", pairing="
      + pairingTime + "ms, level timeout=" + timeoutPerLevelMs + "ms, period=" + periodDurationMs
      + "ms, acceleratedCallsCount=" + acceleratedCallsCount + ", dead nodes=" + nodesDown
      + ", network=" + network.networkLatency.getClass().getSimpleName();
  }

  static class SendSigs extends Network.Message<GSFNode> {
    final BitSet sigs;
    final int level;
    final boolean levelFinished;
    final int size;

    public SendSigs(BitSet sigs, GSFNode.SFLevel l) {
      this.sigs = (BitSet) sigs.clone();
      this.level = l.level;
      // Size = level + bit field + the signatures included
      this.size = 1 + l.expectedSigs() / 8 + 48;
      this.levelFinished = l.verifiedSignatures.equals(l.waitedSigs);
    }

    @Override public int size() {
      return size;
    }

    @Override public void action(GSFNode from, GSFNode to) {
      to.onNewSig(from, this);
    }
  }


  public class GSFNode extends Node {
    final Set<SendSigs> toVerify = new HashSet<>();
    final List<SFLevel> levels = new ArrayList<>();
    final BitSet verifiedSignatures = new BitSet(nodeCount);

    boolean done = false;

    GSFNode() {
      super(network.rd, nb);
      verifiedSignatures.set(nodeId);
    }

    public void initLevel() {
      int roundedPow2NodeCount = MoreMath.roundPow2(nodeCount);
      BitSet allPreviousNodes = new BitSet(nodeCount);
      SFLevel last = new SFLevel();
      levels.add(last);
      for (int l = 1; Math.pow(2, l) <= roundedPow2NodeCount; l++) {
        allPreviousNodes.or(last.waitedSigs);
        last = new SFLevel(last, allPreviousNodes);
        levels.add(last);
      }
    }

    BitSet getLastFinishedLevel() {
      BitSet res = new BitSet();
      SFLevel sfl = levels.get(0);
      boolean done = false;
      while (!done) {
        if (sfl.waitedSigs.equals(sfl.verifiedSignatures)) {
          res.or(sfl.waitedSigs);
          if (sfl.level < levels.size() - 1) {
            sfl = levels.get(sfl.level + 1);
          } else {
            done = true;
          }
        } else {
          done = true;
        }
      }
      return res;
    }


    public void doCycle() {
      // We send the last finished level because if we are more advanced
      //  than our counterparty we can send the full set of signature without risking
      //  a collusion between the set: out set replaces its own set
      BitSet toSend = getLastFinishedLevel();
      boolean previousLevelReceiveOk = true;
      for (SFLevel sfl : levels) {
        sfl.doCycle(toSend, previousLevelReceiveOk); // We send what we collected at the previous levels
        previousLevelReceiveOk = sfl.hasReceivedAll();
        toSend.or(sfl.verifiedSignatures);
      }
    }

    /**
     * At a given level, we send the signatures received from the previous level and we expect the
     * equivalent from our peers of this level.<br/>
     * If nodeId == 0<br/>
     * l0: 0 => send to 1 <br/>
     * l1: 0 1 => send to 2 3 <br/>
     * l2: 0 1 2 3 => send to 4 5 6 7 <br/>
     * l3: 0 1 2 3 4 5 6 7 => send to 8 9 10 11 12 13 14 15 <br/>
     */
    public class SFLevel {
      final int level;
      final List<GSFNode> peers; // The peers when we have all signatures for this level.
      final BitSet waitedSigs; // 1 for the signatures we should have at this level
      final BitSet verifiedSignatures = new BitSet(); // The signatures verified in this level
      final Set<GSFNode> finishedPeers = new HashSet<>();

      /**
       * We're going to contact all nodes, one after the other. That's our position in the peers'
       * list.
       */
      int posInLevel = 0;

      /**
       * Number of message to send for each new update.
       */
      int remainingCalls;

      /**
       * Build a level 0 object. At level 0 need (and have) only our own signature. We have only one
       * peer to contact.
       */
      public SFLevel() {
        level = 0;
        waitedSigs = new BitSet();
        waitedSigs.set(nodeId);
        verifiedSignatures.set(nodeId);
        peers = Collections.emptyList();
        remainingCalls = 0;
      }

      /**
       * Build a level on top of the previous one.
       */
      public SFLevel(SFLevel previousLevel, BitSet allPreviousNodes) {
        this.level = previousLevel.level + 1;

        // Signatures needed to finish the current level are:
        //  sigs of the previous level + peers of the previous level.
        //  If we have all this we have finished this level
        waitedSigs = allSigsAtLevel(this.level);
        waitedSigs.andNot(allPreviousNodes);
        peers = randomSubset(waitedSigs, Integer.MAX_VALUE);
        remainingCalls = peers.size();
      }

      /**
       * That's the number of signatures we have if we have all of them. It's also the number of
       * signatures we're going to send.
       */
      int expectedSigs() {
        return waitedSigs.cardinality();
      }

      /**
       * We start a level if we reached the time out or if we have all the signatures.
       */
      boolean hasStarted(BitSet toSend, boolean previousLevelReceiveOk) {
        if (network.time >= (level) * timeoutPerLevelMs) {
          return true;
        }

        if (toSend.cardinality() >= expectedSigs()) {
          // Equals or greater because we can send more sigs
          //  if the next level is completed already
          return true;
        }

        // The idea here is not to wait for a timeout if
        //  our peers at this level have already finished on their side, even if
        //  some nodes from older levels are not finished yet
        // see https://github.com/ConsenSys/handel/issues/34
       if (previousLevelReceiveOk) return true;

        return false;
      }

      void doCycle(BitSet toSend, boolean previousLevelReceiveOk) {
        if (remainingCalls == 0 || !hasStarted(toSend, previousLevelReceiveOk)) {
          return;
        }

        List<GSFNode> dest = getRemainingPeers(toSend, 1);
        if (!dest.isEmpty()) {
          SendSigs ss = new SendSigs(toSend, this);
          network.send(ss, GSFNode.this, dest.get(0));
        }
      }

      List<GSFNode> getRemainingPeers(BitSet toSend, int peersCt) {
        List<GSFNode> res = new ArrayList<>(peersCt);

        while (peersCt > 0 && remainingCalls > 0) {
          remainingCalls--;

          GSFNode p = peers.get(posInLevel++);
          if (!finishedPeers.contains(p)) {
            res.add(p);
            peersCt--;
          }
          if (posInLevel >= peers.size()) {
            posInLevel = 0;
          }
        }

        return res;
      }

      public boolean hasReceivedAll() {
        BitSet wanted = (BitSet)waitedSigs.clone();
        wanted.and(verifiedSignatures);
        return wanted.cardinality() >= .8 * expectedSigs();
      }
    }

    /**
     * @return all the signatures you should have when this round is finished.
     */
    public BitSet allSigsAtLevel(int round) {
      if (round < 1) {
        throw new IllegalArgumentException("round=" + round);
      }
      BitSet res = new BitSet(nodeCount);
      int cMask = (1 << round) - 1;
      int start = (cMask | nodeId) ^ cMask;
      int end = nodeId | cMask;
      end = Math.min(end, nodeCount - 1);
      res.set(start, end + 1);
      res.set(nodeId, false);

      return res;
    }

    boolean include(BitSet large, BitSet small) {
      BitSet a = (BitSet) large.clone();
      a.and(small);
      return a.equals(small);
    }


    /**
     * If the state has changed we send a message to all. If we're done, we update all our peers: it
     * will be our last message.
     */
    void updateVerifiedSignatures(int level, BitSet sigs) {
      SFLevel sfl = levels.get(level);

      // These lines remove Olivier's optimisation
      //sigs = (BitSet) sigs.clone();
      //sigs.and(sfl.waitedSigs);

      boolean resetRemaining = false;
      if (sigs.cardinality() > sfl.expectedSigs()) {
        // The sender sent us signatures from its next levels as well.
        // It means that it's actually signatures from our first levels
        // For example, level==5, but the remote node sent us [1-5] plus maybe [6-]
        for (int i = 1; i < levels.size() && include(sigs, levels.get(i).waitedSigs); i++) {
          SFLevel l = levels.get(i);
          if (!l.verifiedSignatures.equals(l.waitedSigs)) {
            l.verifiedSignatures.or(l.waitedSigs);
            verifiedSignatures.or(l.waitedSigs);
            resetRemaining = true;
          }
          if (resetRemaining) {
            l.remainingCalls = l.peers.size();
          }
        }
        sigs = (BitSet) sfl.waitedSigs.clone();
      }

      if (sfl.verifiedSignatures.cardinality() > 0 && !sigs.intersects(sfl.verifiedSignatures)) {
        // If the new sigs do not intersect with the previous ones we can aggregate then.
        // Note that technically we could do much more, i.e. searching the history of
        // sigs we have that could intersect
        sigs.or(sfl.verifiedSignatures);
      }

      if (sigs.cardinality() > sfl.verifiedSignatures.cardinality() || resetRemaining) {

        for (int i = sfl.level; i < levels.size(); i++) {
          // We have some new sigs. This is interesting for our level and all the
          //  levels above. So we reset the remainingCalls variable.
          levels.get(i).remainingCalls = levels.get(i).peers.size();
        }

        // We're going to update our common set only if we have more sigs.
        // It's a replacement, not a completion.
        sfl.verifiedSignatures.andNot(sfl.waitedSigs);
        sfl.verifiedSignatures.or(sigs);

        verifiedSignatures.andNot(sfl.waitedSigs);
        verifiedSignatures.or(sigs);

        if (acceleratedCallsCount > 0) {
          // See if we completed this level. Does it finish something?
          // If so, we're going to send immediately an update to 'acceleratedCallsCount' nodes
          // It makes the protocol faster for the branches that are complete.
          BitSet bestToSend = getLastFinishedLevel();
          while (include(bestToSend, sfl.waitedSigs) && sfl.level < levels.size() - 1) {
            sfl = levels.get(sfl.level + 1);
            SendSigs sendSigs = new SendSigs(bestToSend, sfl);
            List<GSFNode> peers =
              sfl.getRemainingPeers(sfl.verifiedSignatures, acceleratedCallsCount);
            if (!peers.isEmpty()) {
              network.send(sendSigs, this, peers);
            }
          }
        }
        if (doneAt == 0 && verifiedSignatures.cardinality() >= threshold) {
          doneAt = network.time;

          // todo: in a byzantine context we need to continue longer to send what we
          //  have to all nodes
          // done = true;
        }
      }
    }

    private List<GSFNode> randomSubset(BitSet nodes, int nodeCt) {
      List<GSFNode> res = new ArrayList<>(nodes.cardinality());
      for (int pos, cur = nodes.nextSetBit(0);
           cur >= 0; pos = cur + 1, cur = nodes.nextSetBit(pos)) {
        res.add(network.getNodeById(cur));
      }

      Collections.shuffle(res, network.rd);
      if (res.size() > nodeCt) {
        return res.subList(0, nodeCt);
      } else {
        return res;
      }
    }


    /**
     * Nothing much to do when we receive a sig set: we just add it to our toVerify list.
     */
    void onNewSig(GSFNode from, SendSigs ssigs) {
      SFLevel sfl = levels.get(ssigs.level);
      toVerify.add(ssigs);
      if (ssigs.levelFinished) {
        sfl.finishedPeers.add(from);
      }
    }

    public void checkSigs() {
      SendSigs best = null;
      int levelFinished = Integer.MAX_VALUE;
      Iterator<SendSigs> it = toVerify.iterator();

      while (it.hasNext() && levelFinished > 2) {
        SendSigs cur = it.next();
        SFLevel l = levels.get(cur.level);

        if (l.expectedSigs() == l.verifiedSignatures.cardinality()) {
          // We have already completed this level.
          it.remove();
          continue;
        }

        if (include(verifiedSignatures, cur.sigs)) {
          it.remove();
          continue;
        }

        if (best == null) {
          best = cur;
        }

        if (cur.sigs.cardinality() > l.expectedSigs()) {
          best = cur;
          levelFinished = 2;
          continue;
        }

        if (!cur.sigs.intersects(l.verifiedSignatures)) {
          if (cur.sigs.cardinality() + l.verifiedSignatures.cardinality()
            == l.waitedSigs.cardinality()) {
            if (levelFinished > cur.level) {
              best = cur;
              levelFinished = cur.level;
            }
          }
        }
      }

      if (best != null) {
        toVerify.remove(best);
        final SendSigs tBest = best;
        network.registerTask(() -> GSFNode.this.updateVerifiedSignatures(tBest.level, tBest.sigs),
          network.time + pairingTime * 2, GSFNode.this);
      }
    }


    @Override public String toString() {
      return "GSFNode{" + "nodeId=" + nodeId + ", doneAt=" + doneAt + ", sigs=" + verifiedSignatures
        .cardinality() + ", msgReceived=" + msgReceived + ", msgSent=" + msgSent + ", KBytesSent="
        + bytesSent / 1024 + ", KBytesReceived=" + bytesReceived / 1024 + '}';
    }
  }

  @Override public Protocol copy() {
    return new GSFSignature(nodeCount, threshold, pairingTime, timeoutPerLevelMs, periodDurationMs,
      acceleratedCallsCount, nodesDown, nb.copy(), this.network.networkLatency);
  }

  public void init() {
    for (int i = 0; i < nodeCount; i++) {
      final GSFNode n = new GSFNode();
      network.addNode(n);
    }

    for (int setDown = 0; setDown < nodesDown; ) {
      int down = network.rd.nextInt(nodeCount);
      Node n = network.allNodes.get(down);
      if (!n.down && down != 1) {
        // We keep the node 1 up to help on debugging
        n.down = true;
        setDown++;
      }
    }

    for (GSFNode n : network.allNodes) {
      if (!n.down) {
        n.initLevel();
        network.registerPeriodicTask(n::doCycle, 1, periodDurationMs, n);
        network.registerConditionalTask(n::checkSigs, 1, pairingTime, n,
          () -> !n.toVerify.isEmpty(), () -> !n.done);
      }
    }
  }

  @Override public Network<?> network() {
    return network;
  }

  public static void sigsPerTime() {
    int nodeCt = 32768 / 16;
    NetworkLatency nl = new NetworkLatency.NetworkLatencyByDistance();
    Node.NodeBuilder nb = new Node.NodeBuilderWithRandomPosition();

    int ts = (int) (0.66 * nodeCt);
    GSFSignature p = new GSFSignature(nodeCt, ts, 3, 100, 10, 10, 100, nb, nl);

    StatsHelper.StatsGetter sg = new StatsHelper.StatsGetter() {
      final List<String> fields = new StatsHelper.SimpleStats(0, 0, 0).fields();

      @Override public List<String> fields() {
        return fields;
      }

      @Override public StatsHelper.Stat get(List<? extends Node> liveNodes) {
        return StatsHelper.getStatsOn(liveNodes,
          n -> ((GSFNode) n).verifiedSignatures.cardinality());
      }
    };

    ProgressPerTime ppt = new ProgressPerTime(p, "", "number of signatures", sg, 1);

    Predicate<Protocol> contIf = p1 -> {
      for (Node n : p1.network().allNodes) {
        GSFNode gn = (GSFNode) n;
        // All up nodes must have reached the threshold
        if (!n.down && gn.verifiedSignatures.cardinality() < p.threshold) {
          return true;
        }
      }
      return false;
    };

    ppt.run(contIf);
  }

  public static void main(String... args) {
    sigsPerTime();
  }
}
