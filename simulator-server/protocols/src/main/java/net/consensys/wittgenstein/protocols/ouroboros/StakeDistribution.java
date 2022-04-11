package net.consensys.wittgenstein.protocols.ouroboros;

import net.consensys.wittgenstein.protocols.solana.SolanaConfig;
import net.consensys.wittgenstein.protocols.utils.StakeDistributionUtil;
import net.consensys.wittgenstein.protocols.utils.VRFLeaderSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StakeDistribution {

    private Random rd;
    private StakeDistributionUtil stakeDistributionUtil;
    private final String CONFIG_FILE = "CardanoStake--2022-04-04.csv";
    private final OuroborosConfig ouroborosConfig;
    public List<Integer> nodesStake;
    public int totalStake;
    public List<Double> nodesProbability;
    public VRFLeaderSelection vrfLeaderSelection;

    StakeDistribution(OuroborosConfig ouroborosConfig, Random rd) {
        this.ouroborosConfig = ouroborosConfig;
        this.rd = rd;
        this.stakeDistributionUtil = new StakeDistributionUtil(rd);
        this.nodesStake = (ouroborosConfig.uniformStakeDistribution)
                ? stakeDistributionUtil.uniformDistribution(ouroborosConfig.networkSize)
                : stakeDistributionUtil.readStakeDistributionFromConfigurationFile(CONFIG_FILE, ouroborosConfig.networkSize);

        this.totalStake = nodesStake.stream().mapToInt(value -> value).sum();
        this.nodesProbability = nodesStake.stream().mapToDouble(value -> (double) value / totalStake).boxed().collect(Collectors.toList());
        updateVRF(0);
    }

    public void updateVRF(int epoch) {
        this.vrfLeaderSelection = new VRFLeaderSelection(epoch, IntStream.range(0, ouroborosConfig.networkSize).boxed().collect(Collectors.toList()), nodesProbability);
    }

    public void updateStakeDistribution(int epoch) {
        nodesStake = stakeDistributionUtil.updateStakeDistribution(epoch, nodesStake);
        nodesProbability = stakeDistributionUtil.updateStakeProbability(epoch, nodesStake);
        totalStake = nodesStake.stream().mapToInt(i->i).sum();
    }
}
