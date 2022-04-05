package net.consensys.wittgenstein.protocols.solana;

import net.consensys.wittgenstein.protocols.utils.StakeDistributionUtil;
import net.consensys.wittgenstein.protocols.utils.VRFLeaderSelection;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stake distribution for Solana. This is a GOD object due to simulation effectivity. Correctly every node should
 * possess own copy, but that is to memory consuming for simulation purposes.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class StakeDistribution {
    private int totalStake;
    private List<Integer> nodesStake;
    private List<Double> nodesProbability;
    private Random rd;
    public StakeDistributionUtil stakeDistributionUtil;
    private final String CONFIG_FILE = "SolanaStake-2022-02-22.csv";
    public VRFLeaderSelection vrfLeaderSelection;
    public final SolanaConfig solanaConfig;

    StakeDistribution(SolanaConfig solanaConfig, Random rd) {
        this.rd = rd;
        this.stakeDistributionUtil = new StakeDistributionUtil(rd);
        this.solanaConfig = solanaConfig;
        if (solanaConfig.uniformStakeDistribution) {
            nodesStake = stakeDistributionUtil.uniformDistribution(solanaConfig.networkSize);
        }
        else {
            nodesStake = stakeDistributionUtil.readStakeDistributionFromConfigurationFile(CONFIG_FILE, solanaConfig.networkSize);
        }

        this.totalStake = nodesStake.stream().mapToInt(value -> value).sum();
        this.nodesProbability = nodesStake.stream().mapToDouble(value -> (double) value / totalStake).boxed().collect(Collectors.toList());

        updateVRF(0);
    }

    public void updateVRF(int epoch) {
        this.vrfLeaderSelection = new VRFLeaderSelection(epoch, IntStream.range(0, solanaConfig.networkSize).boxed().collect(Collectors.toList()), nodesProbability);
    }

    public int getTotalStake() {
        return totalStake;
    }

    public void setTotalStake(int totalStake) {
        this.totalStake = totalStake;
    }

    public List<Integer> getNodesStake() {
        return nodesStake;
    }

    public void setNodesStake(List<Integer> nodesStake) {
        this.nodesStake = nodesStake;
    }

    public List<Double> getNodesProbability() {
        return nodesProbability;
    }

    public void setNodesProbability(List<Double> nodeLeaderProbability) {
        this.nodesProbability = nodeLeaderProbability;
    }

    public void setStake(int node, int newStake) {
        int oldStake = nodesStake.get(node);
        nodesStake.set(node, newStake);
        totalStake = totalStake - oldStake + newStake;
        nodesProbability.set(node, (double)newStake / totalStake);
    }

    public class Stake {
        public int totalStake;
        public int nodeStake;
        public double nodeProbability;

        public Stake(int totalStake, int nodeStake, double nodeProbability) {
            this.totalStake = totalStake;
            this.nodeStake = nodeStake;
            this.nodeProbability = nodeProbability;
        }
    }

    public Stake getStake(int node) {
        return new Stake(totalStake, nodesStake.get(node), nodesProbability.get(node));
    }

}
