package net.consensys.wittgenstein.protocols.harmony.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.consensys.wittgenstein.core.Network;
import net.consensys.wittgenstein.protocols.harmony.Harmony;
import net.consensys.wittgenstein.protocols.harmony.HarmonyNode;
import net.consensys.wittgenstein.protocols.harmony.Shard;
import net.consensys.wittgenstein.protocols.harmony.StakeDistribution;
import net.consensys.wittgenstein.protocols.solana.Solana;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CsvDumper {

    private final String OUTPUT_DIR = "output";
    private ObjectMapper objectMapper = new ObjectMapper();
    private final List<SlotStats> epochSlotStats = new ArrayList<>();
    private final OutputInfo outputInfo = new OutputInfo();
    MongoClient mongoClient;
    JacksonMongoCollection<SlotStats> collectionSlotStats;
    JacksonMongoCollection<Leader> collectionLeaders;
    JacksonMongoCollection<StakeStats> collectionStakeStats;
    private final String database = "simulator";
    private static final Logger logger;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Harmony.class.getName());
        Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.protocol.command").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.protocol.operation").setLevel(Level.SEVERE);
    }

    public CsvDumper() throws UnknownHostException {
        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress("mongodb"))))
                        .build());
        mongoClient.getDatabase(database).drop();
        collectionSlotStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Epochs", SlotStats.class, UuidRepresentation.STANDARD);
        collectionLeaders = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "Leaders", Leader.class, UuidRepresentation.STANDARD);
        collectionStakeStats = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoClient, database, "StakeStats", StakeStats.class, UuidRepresentation.STANDARD);
    }

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
        //createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "epochs"));
        if (epochSlotStats.isEmpty()) return;
        logger.info(String.format("Dump epochs: %d", epochSlotStats.size()));

        collectionSlotStats.insertMany(epochSlotStats);

//        String path = writeToCsv(Paths.get(OUTPUT_DIR, "epochs", "epoch"+epoch+".csv"), epochSlotStats);
//        if (!outputInfo.epochs.contains(path)) {
//            outputInfo.epochs.add(path);
//        }
        epochSlotStats.clear();
    }

    public void dumpLeaders(List<Leader> leaders) {
        //createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "leaders"));
        //outputInfo.leaders = writeToCsv(Paths.get(OUTPUT_DIR, "leaders.csv"), leaders);
        collectionLeaders.insertMany(leaders);
    }

    public void dumpEpochStake(int epoch, StakeDistribution stakeDistribution, Network<HarmonyNode> network) {
        //createDirectoryIfNotExist(Paths.get(OUTPUT_DIR, "stake"));
        List<StakeStats> stakeStats = new ArrayList<>();
        for (int node = 0; node < stakeDistribution.getNodesStake().size(); node++) {
            int stake = stakeDistribution.getNodesStake().get(node);
            int tokens = stakeDistribution.getNodesTokens().get(node);
            boolean byzantine = network.allNodes.get(node).byzantine;
            Map<Integer, Long> shardTokens = new HashMap<>();
            for (Shard shard : stakeDistribution.shards) {
                shardTokens.put(shard.shardId, shard.stakeholders.getOrDefault(node, 0L));
            }
            stakeStats.add(new StakeStats(node, epoch, stake, tokens, byzantine, shardTokens));
        }
        //String path = writeToJson(Paths.get(OUTPUT_DIR, "stake", "epoch"+epoch+".json"), stakeStats);
        //outputInfo.stake.add(path);
        collectionStakeStats.insertMany(stakeStats);
    }

    public OutputInfo outputInfo() {
        return outputInfo;
    }
}
