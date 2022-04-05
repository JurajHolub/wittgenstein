package net.consensys.wittgenstein.protocols.utils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility helps to manipulate with stake.
 */
public class StakeDistributionUtil {
    private final Random rd;

    public StakeDistributionUtil(Random rd) {
        this.rd = rd;
    }

    /**
     * It read N (size) real stakes from config file.
     * @param fileName Expected values = {CardanoStake--2022-04-04.csv, HarmonyStake-2022-02-24.csv, SolanaStake-2022-02-22.csv}
     * @param size Size of network
     * @return List of stakes with size 'size'.
     */
    public List<Integer> readStakeDistributionFromConfigurationFile(String fileName, int size) {
        List<Integer> nodesStake = new ArrayList<>();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        ObjectReader objectReader = new CsvMapper().readerFor(StakeDto.class).with(schema);
        try {
            MappingIterator<StakeDto> userMappingIterator =
                    objectReader.readValues(getClass().getClassLoader().getResource(fileName));
            nodesStake = userMappingIterator.readAll().stream()
                    .mapToInt(s -> Integer.parseInt(s.getStake())).boxed().limit(size).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // if there is not enough data in config file
        int averageStake = (nodesStake.size() == 0)
                ? 0
                : nodesStake.stream().mapToInt(i->i).sum() / nodesStake.size();

        if (size - nodesStake.size() > 0) {
            int carryOver = size - nodesStake.size();
            for (int i = 0; i < carryOver; i++) {
                nodesStake.add(averageStake);
            }
        }
        return nodesStake;
    }

    /**
     * Genereate N (size) uniformly distributed values.
     */
    public List<Integer> uniformDistribution(int size) {
        int stdDeviation = 20;
        int mean = 100;
        return IntStream.range(0, size).map(i ->
                180 - (int)(rd.nextGaussian()*stdDeviation + mean)
        ).boxed().collect(Collectors.toList());
    }

    public List<Integer> updateStakeDistribution(int epoch, List<Integer> stakeDistribution) {
        for (int node = 0; node < stakeDistribution.size(); node++) {
            int oldStake = stakeDistribution.get(node);
            double change = oldStake / 100.0;
            int newStake = oldStake + (int) (rd.nextGaussian() * change);
            stakeDistribution.set(node, newStake);
        }
        return stakeDistribution;
    }

    public List<Double> updateStakeProbability(int epoch, List<Integer> stakeDistribution, List<Double> nodesProbability) {
        int totalStake = stakeDistribution.stream().mapToInt(i->i).sum();
        for (int node = 0; node < nodesProbability.size(); node++) {
            double pst = stakeDistribution.get(node) / (double)totalStake;
            nodesProbability.set(node, pst);
        }
        return nodesProbability;
    }

}
