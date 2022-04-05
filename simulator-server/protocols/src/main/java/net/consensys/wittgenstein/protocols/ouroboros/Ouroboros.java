package net.consensys.wittgenstein.protocols.ouroboros;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.consensys.wittgenstein.core.*;
import net.consensys.wittgenstein.core.geoinfo.Geo;
import net.consensys.wittgenstein.core.geoinfo.GeoAllCities;
import net.consensys.wittgenstein.protocols.ouroboros.output.OutputDumper;
import net.consensys.wittgenstein.protocols.utils.AliasMethod;
import net.consensys.wittgenstein.protocols.utils.DosAttackUtil;
import net.consensys.wittgenstein.tools.CSVLatencyReader;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Simulation of Ouroboros (Cardano) consensus protocol.
 * @author Juraj Holub <xholub40@vutbr.cz>
 */
public class Ouroboros  implements Protocol {

    private static final Logger logger;
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$s] %5$s %n");
        logger = Logger.getLogger(Ouroboros.class.getName());
    }

    private final OuroborosConfig ouroborosConfig;
    private final P2PNetwork<OuroborosNode> network;
    private final NodeBuilder nb;
    private final List<Integer> leaderSchedule;
    private final StakeDistribution stakeDistribution;
    private final OutputDumper outputDumper;
    private final DosAttackUtil dosAttackUtil = new DosAttackUtil();

    public Ouroboros(OuroborosConfig ouroborosConfig) throws UnknownHostException {
        this.ouroborosConfig = ouroborosConfig;
        Geo geoAllCities = new GeoAllCities();
        CSVLatencyReader lr = new CSVLatencyReader();
        this.nb = new NodeBuilder.NodeBuilderWithCity(lr.cities(), geoAllCities);
        this.network = new P2PNetwork<>(ouroborosConfig.p2pConnectionCount, ouroborosConfig.p2pMinimum);
        this.leaderSchedule = new ArrayList<>();
        this.stakeDistribution = new StakeDistribution(ouroborosConfig, network.rd);
        this.outputDumper = new OutputDumper(ouroborosConfig);
        this.network.setNetworkLatency(new NetworkLatency.NetworkLatencyByCityWJitter());
    }

    @Override
    public Network<OuroborosNode> network() {
        return network;
    }

    @Override
    public Protocol copy() {
        return null;
    }

    @Override
    public void init() {
        for (int i = 0; i < ouroborosConfig.networkSize; i++) {
            OuroborosNode node = new OuroborosNode(
                network.rd,
                nb,
                false,
                ouroborosConfig,
                network,
                leaderSchedule,
                outputDumper,
                stakeDistribution
            );
            network.addNode(node);
        }
        network.setPeers();
        outputDumper.dumpP2PNetwork(network.allNodes);

        setLeadersForEpoch(0);
    }

    public void calculateLeaderSchedule(int epoch) {
        leaderSchedule.clear();

        AliasMethod aliasMethod = new AliasMethod(
            stakeDistribution.nodesProbability,
            new Random(epoch+42) // +42 to prevent 0 seed at epoch 0.
        );

        for (int i = 0; i < ouroborosConfig.epochDurationInSlots; i++) {
            leaderSchedule.add(aliasMethod.next());
        }

    }

    public void setLeadersForEpoch(int epoch) {
        if (ouroborosConfig.vrfLeaderSelection) {
            this.stakeDistribution.updateVRF(epoch);
        }
        else {
            calculateLeaderSchedule(epoch);
        }
    }

    abstract static class AOuroborosNode extends P2PNode<OuroborosNode> {

        protected final transient OuroborosConfig ouroborosConfig;
        protected final transient Random rd;
        protected final transient P2PNetwork<OuroborosNode> network;
        protected final transient List<Integer> leaderSchedule;
        protected final transient OutputDumper outputDumper;
        protected final transient StakeDistribution stakeDistribution;

        public AOuroborosNode(
                Random rd,
                NodeBuilder nb,
                boolean byzantine,
                OuroborosConfig ouroborosConfig,
                P2PNetwork<OuroborosNode> network,
                List<Integer> leaderSchedule,
                OutputDumper outputDumper,
                StakeDistribution stakeDistribution
        ) {
            super(rd, nb, byzantine);
            this.rd = rd;
            this.ouroborosConfig = ouroborosConfig;
            this.network = network;
            this.leaderSchedule = leaderSchedule;
            this.outputDumper = outputDumper;
            this.stakeDistribution = stakeDistribution;
        }
    }

    public void simulate() {
        for (int epoch = 0; epoch < ouroborosConfig.numberOfEpochs; epoch++) {
            logger.info(String.format("Start epoch %d/%d", epoch+1, ouroborosConfig.numberOfEpochs));
            simulateEpoch(epoch);
        }
        network().run(5000); // enough time to receive last messages
    }

    public void simulateEpoch(int epoch) {
        outputDumper.dumpLeaderSchedule(epoch, leaderSchedule);
        outputDumper.dumpStake(epoch, stakeDistribution.nodesStake);
        int intervalForLoggingInSlots = 100;
        for (int slot = 0; slot < ouroborosConfig.epochDurationInSlots; slot++) {
            if (slot % intervalForLoggingInSlots == 0) {
                logger.info(String.format("Simulate epoch %d/%d, Slot %d/%d [%.2f%%]",
                    epoch+1,
                    ouroborosConfig.numberOfEpochs,
                    slot, ouroborosConfig.epochDurationInSlots,
                    (double)slot / ouroborosConfig.epochDurationInSlots * 100)
                );
            }

//            if (network.rd.nextBoolean()) {
//                float part = network.rd.nextFloat();
//                if (part != 0) {
//                    network.partition(part);
//                }
//            }
//            if (network.rd.nextBoolean()) {
//                network.endPartition();
//            }

            network.runMs(ouroborosConfig.slotDurationInMs);
            simulateSlot(epoch, slot);
        }
        stakeDistribution.updateStakeDistribution(epoch+1);
        setLeadersForEpoch(epoch + 1);
    }

    public void simulateSlot(int epoch, int slot) {
        int slotLeader = (ouroborosConfig.vrfLeaderSelection)
                ? stakeDistribution.vrfLeaderSelection.chooseSlotLeader(slot)
                : leaderSchedule.get(slot);
        OuroborosNode leader = network.allNodes.get(slotLeader);

        if (leader.underDos) {
            network.allNodes.forEach(node -> node.outputDumper.dumpSlot(new Block(
                -1,
                slot,
                epoch,
                0,
                0,
                0,
                network.time
            ), node, network.time));
        }
        else {
            leader.onSlotEnd(epoch, slot);
        }
    }

    public void prepareDosAttack() {
        if (ouroborosConfig.numberOfNodesUnderDos == 0) return;

        List<Integer> leaders = (ouroborosConfig.vrfLeaderSelection)
                ? dosAttackUtil.bestLeadersToAttackWithVDF(stakeDistribution.nodesStake, ouroborosConfig.numberOfNodesUnderDos)
                : dosAttackUtil.bestLeadersToAttackWithSchedule(leaderSchedule, ouroborosConfig.numberOfNodesUnderDos);

        for (int nodeId : leaders) {
            network.allNodes.get(nodeId).underDos = true;
        }
    }

    public static void run(OuroborosConfig ouroborosConfig) throws IOException {
        Ouroboros ouroboros = new Ouroboros(ouroborosConfig);
        ouroboros.init();
        ouroboros.prepareDosAttack();

        logger.info("Start simulation of Ouroboros.");
        ObjectMapper objectMapper = new ObjectMapper();
        logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ouroborosConfig));
        ouroboros.simulate();
        logger.info("End simulation of Ouroboros.");
    }
}
