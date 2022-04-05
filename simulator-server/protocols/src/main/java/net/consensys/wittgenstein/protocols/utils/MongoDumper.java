package net.consensys.wittgenstein.protocols.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.consensys.wittgenstein.protocols.harmony.Harmony;


import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility encapsulates operations with MongoDB.
 */
public class MongoDumper {

    protected final MongoClient mongoClient;
    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected final String database = "simulator";

    protected static final Logger logger;
    /** This omits Mongo spam in logger. */
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Harmony.class.getName());
        Logger.getLogger("org.mongodb.driver.cluster").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.protocol.command").setLevel(Level.SEVERE);
        Logger.getLogger("org.mongodb.driver.protocol.operation").setLevel(Level.SEVERE);
    }

    /**
     * Create mongo client with connection to database given in the sharedConfig.mongoServerAddress and drop it
     * (so it is prepared for clean simulation run).
     */
    public MongoDumper(SharedConfig sharedConfig) throws UnknownHostException {
        mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress(sharedConfig.mongoServerAddress))))
                        .build());
        mongoClient.getDatabase(database).drop();
    }
}
