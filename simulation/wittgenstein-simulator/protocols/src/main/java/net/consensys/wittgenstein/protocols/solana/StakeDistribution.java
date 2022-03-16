package net.consensys.wittgenstein.protocols.solana;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import net.consensys.wittgenstein.protocols.utils.StakeDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StakeDistribution {
    private int totalStake;
    private List<Integer> nodesStake;
    private List<Double> nodesProbability;

    StakeDistribution(int totalStake, List<Integer> nodesStake, List<Double> nodesProbability) {
        this.totalStake = totalStake;
        this.nodesStake = nodesStake;
        this.nodesProbability = nodesProbability;
    }

    StakeDistribution(int size) {
        readStakeDistributionFromConfigurationFile(size);

        size -= nodesStake.size();
        IntStream.range(0, size).forEach(i-> nodesStake.add(0));

        this.totalStake = nodesStake.stream().mapToInt(value -> value).sum();
        this.nodesProbability = nodesStake.stream().mapToDouble(value -> (double) value / totalStake).boxed().collect(Collectors.toList());
    }

    private void readStakeDistributionFromConfigurationFile(int size) {
        String fileName = "SolanaStake-2022-02-22.csv";
        File csvFile = new File(fileName);
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader objectReader = new CsvMapper().readerFor(StakeDto.class).with(schema);
        try {
            MappingIterator<StakeDto> userMappingIterator = objectReader.readValues(csvFile);
            this.nodesStake = userMappingIterator.readAll().stream().mapToInt(s -> Integer.parseInt(s.getStake())).boxed().limit(size).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
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
