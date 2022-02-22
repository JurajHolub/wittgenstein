package net.consensys.wittgenstein.protocols.solana;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NodeStateDump {

    public int networkSize;
    private final String OUTPUT_DIR = "output/";
    private ObjectMapper objectMapper = new ObjectMapper();

    public NodeStateDump(int networkSize) {
        this.networkSize = networkSize;
    }


    private static class SlotStats {

        public int slot;
        public int txCounterVote;
        public int receivedVotingPower;
        public int totalVotingPower;
        public int txCounterNonVote;
        public int arriveTime;

        public SlotStats(int slot, SlotData slotData) {
            this.slot = slot;
            this.txCounterVote = slotData.txCounterVote;
            this.receivedVotingPower = slotData.receivedVotingPower;
            this.totalVotingPower = slotData.totalVotingPower;
            this.txCounterNonVote = slotData.txCounterNonVote;
            this.arriveTime = slotData.arriveTime;
        }
    }

    public static class NodeStake{
        public int stake;
        public int node;

        public NodeStake(int node, int stake) {
            this.stake = stake;
            this.node = node;
        }
    }

    public static class Leaders {
        public int slot;
        public int leaderNode;

        public Leaders(int slot, int leaderNode) {
            this.slot = slot;
            this.leaderNode = leaderNode;
        }
    }

    public void cleanData() {
        try {
            FileUtils.deleteDirectory(new File(OUTPUT_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createDirectoryIfNotExist(String directory) {
        try {
            Files.createDirectories(Paths.get(directory));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return directory;
    }

    private void writeToCsv(String file, Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            JsonNode jsonTree = objectMapper.readTree(bytes);
            CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
            JsonNode firstObject = jsonTree.elements().next();
            firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
            CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
            CsvMapper csvMapper = new CsvMapper();
            csvMapper.writerFor(JsonNode.class)
                    .with(csvSchema)
                    .writeValue(new File(file+".csv"), jsonTree);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        catch(IOException ioe) {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }

    public void dumpSlotState(Solana.SolNode node, int epoch, Map<Integer, SlotData> slotMds) {
        String directory = createDirectoryIfNotExist(OUTPUT_DIR + "epochs/epoch" + epoch);

        List<SlotStats> slots = slotMds.entrySet().stream().map(e -> new SlotStats(e.getKey(), e.getValue())).collect(Collectors.toList());
        writeToCsv(directory + "/node"+node.nodeId, slots);
    }

    public void dumpStakeState(int epoch, StakeDistribution stakeDistribution, List<Integer> leaderSchedule, List<Solana.SolNode> nodes) {
        String directory = createDirectoryIfNotExist(OUTPUT_DIR + "stake");
        List<NodeStake> stake = nodes.stream().map(n -> new NodeStake(n.nodeId, stakeDistribution.getStake(n.nodeId).nodeStake)).collect(Collectors.toList());
        writeToCsv(directory + "/epoch"+epoch, stake);

        directory = createDirectoryIfNotExist(OUTPUT_DIR + "leaders");
        List<Leaders> leaders = IntStream.range(0, leaderSchedule.size()).mapToObj(slot -> new Leaders(slot, leaderSchedule.get(slot))).collect(Collectors.toList());
        writeToCsv(directory + "/epoch"+epoch, leaders);
    }
}
