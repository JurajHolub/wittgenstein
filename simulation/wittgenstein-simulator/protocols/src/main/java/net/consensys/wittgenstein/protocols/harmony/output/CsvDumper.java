package net.consensys.wittgenstein.protocols.harmony.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Table;
import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.protocols.harmony.BlockSigners;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;
import net.consensys.wittgenstein.protocols.harmony.Shard;
import net.consensys.wittgenstein.protocols.harmony.StakeDistribution;
import net.consensys.wittgenstein.protocols.solana.NodeStateDump;
import net.consensys.wittgenstein.protocols.solana.SlotData;
import net.consensys.wittgenstein.protocols.solana.Solana;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CsvDumper {

    private final String OUTPUT_DIR = "output";
    private ObjectMapper objectMapper = new ObjectMapper();
    private final List<SlotStats> epochSlotStats = new ArrayList<>();
    private final OutputInfo outputInfo = new OutputInfo();

    public void cleanData() {
        try {
            FileUtils.deleteDirectory(new File(OUTPUT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDirectoryIfNotExist(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <E> String writeToCsv(Path file, List<E> value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            JsonNode jsonTree = objectMapper.readTree(bytes);
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
            JsonNode firstObject = jsonTree.elements().next();
            firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
            CsvMapper csvMapper = new CsvMapper();
            if (!file.toFile().exists()) {
                CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
                csvMapper.writerFor(JsonNode.class)
                    .with(csvSchema)
                    .writeValue(file.toFile(), jsonTree);
            }
            else {
                CsvSchema csvSchema = csvSchemaBuilder.build().withoutHeader();
                csvMapper.writerFor(JsonNode.class)
                    .with(csvSchema)
                    .writeValue(new FileOutputStream(file.toFile(), true), jsonTree);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.toAbsolutePath().toString();
    }

    private <E> String writeToJson(Path file, List<E> value) {
        try {
            objectMapper.writeValue(file.toFile(), value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.toAbsolutePath().toString();
    }

    public void dumpSlot(SlotStats slotStats) {
        epochSlotStats.add(slotStats);
    }

    public void dumpEpoch(int epoch) {
        createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "epochs"));
        if (epochSlotStats.isEmpty()) return;
        String path = writeToCsv(Paths.get(OUTPUT_DIR, "epochs", "epoch"+epoch+".csv"), epochSlotStats);
        outputInfo.epochs.add(path);
        epochSlotStats.clear();
    }

    public void dumpLeaders(List<Leader> leaders) {
        createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "leaders"));
        outputInfo.leaders = writeToCsv(Paths.get(OUTPUT_DIR, "leaders.csv"), leaders);
    }

    public void dumpEpochStake(int epoch, StakeDistribution stakeDistribution, Network<HarmonyNode> network) {
        createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "stake"));
        List<StakeStats> stakeStats = new ArrayList<>();
        for (int node = 0; node < stakeDistribution.getNodesStake().size(); node++) {
            int stake = stakeDistribution.getNodesStake().get(node);
            int tokens = stakeDistribution.getNodesTokens().get(node);
            boolean byzantine = network.allNodes.get(node).byzantine;
            Map<Integer, Long> shardTokens = new HashMap<>();
            for (Shard shard : stakeDistribution.shards) {
                shardTokens.put(shard.shardId, shard.stakeholders.getOrDefault(node, 0L));
            }
            stakeStats.add(new StakeStats(node, stake, tokens, byzantine, shardTokens));
        }
        String path = writeToJson(Paths.get(OUTPUT_DIR, "stake", "epoch"+epoch+".json"), stakeStats);
        outputInfo.stake.add(path);
    }

    public OutputInfo outputInfo() {
        return outputInfo;
    }
}
