import json
import pandas as pd
import requests
from matplotlib import pyplot as plt

import logger
from scenarios.scenario import Scenario


class Scenario05(Scenario):
    """
    DDoS leader
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.tpb = {}

    def simulate(self):
        nodes = 1000
        slots = 1000
        token_lambda = 600
        shards = 4
        for vrfFeature in [False, True]:
            parameters = {
                "epochDurationInSlots": slots,
                "numberOfEpochs": 1,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": nodes,
                "numberOfShards": shards,
                "expectedTxPerBlock": 1500,
                "byzantineNodes": 0,
                "lambda": token_lambda,
                "ddosAttacks": True,
                "shardDoSMax": 5,
                "vrfLeaderSelection": vrfFeature,
                "mongoServerAddress": self.mongoserver,
                "uniformStakeDistribution": True
            }

            logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')

            db = self.get_data_from_mongo()
            df = pd.DataFrame(list(db.Epochs.find()))

            self.tpb[vrfFeature] = []
            for slot in range(slots):
                slot_df = df[df['slot'] == slot]
                self.tpb[vrfFeature].append(slot_df['transactions'].sum() / nodes)

    def analyze(self):
        fig = plt.figure()
        ax = pd.DataFrame({
            'Zoznam vodcov': self.tpb[False][:550],
            'VRF': self.tpb[True][:550],
        }, index=range(550)).plot.line(legend=True)
        ax.set_ylabel('Transakcie')
        ax.set_xlabel('Slot')
        ax.grid(axis="y", linestyle='--')
        fig.tight_layout()
        self.save_plot(f'harmony-scenario05')
