import json
import os
import re
import statistics
import seaborn as sns
import pandas as pd
import requests
from matplotlib import pyplot as plt, gridspec

import logger
from scenarios.scenario import Scenario


class Scenario04(Scenario):
    """
    DDoS leader
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        nodes = 1000
        slots = 1000
        token_lambda = 600
        shards = 4
        parameters = {
            "epochDurationInSlots": slots,
            "numberOfEpochs": 1,
            "vdfInSlots": 5,
            "txSizeInBytes": 670,  # see bitcoin-block-size.py
            "blockHeaderSizeInBytes": 80,
            "networkSize": nodes,
            "numberOfShards": shards,
            "expectedTxPerBlock": 600,
            "byzantineNodes": 0,
            "lambda": token_lambda,
            "ddosAttacks": True,
        }

        logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
        response = requests.post(self.harmony_endpoint, json=parameters)
        logger.logging.info(f'Simulation result: {response}')

        db = self.get_data_from_mongo()
        df = pd.DataFrame(list(db.Epochs.find()))

        self.tpb = []
        for slot in range(slots):
            slot_df = df[df['slot'] == slot]
            self.tpb.append(slot_df['transactions'].sum() / nodes)

    def analyze(self):
        fig = plt.figure()
        ax = pd.DataFrame({
            'Tx': self.tpb[:550]
        }, index=range(550)).plot.line(legend=False)
        ax.set_ylabel('Transakcie')
        ax.set_xlabel('Slot')
        ax.grid(axis="y", linestyle='--')
        fig.tight_layout()
        self.save_plot(f'ddos-attack')
