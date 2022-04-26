import json
import math
import statistics

import pandas as pd
import numpy as np
import requests
from pymongo import MongoClient

import logger
from scenarios.scenario import Scenario
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
import seaborn as sns


class Scenario03(Scenario):
    """
    Check traffic size in compare to network size (bytes throughput)
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}
        self.leaders = {}
        self.index = []

    def simulate(self):
        epochDurationInSlots = 300
        tmp = []
        for numberOfNodes in [1000, 2000, 5000, 10_000]:
            expectedTxPerBlock = numberOfNodes * 3
            parameters = {
                "slotDurationInMs": 400,
                "epochDurationInSlots": epochDurationInSlots,
                "validatorReliability": 100,
                "expectedTxPerBlock": expectedTxPerBlock,
                "networkSize": numberOfNodes,
                "numberOfEpochs": 1,
                "numberOfNodesUnderAttack": 0,
                "uniformStakeDistribution": True,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "mongoServerAddress": self.mongoserver
            }

            logger.logging.info(
                f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.solana_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')

            client = MongoClient()
            #leaders = pd.DataFrame(list(client.simulator.Leaders.find()))
            epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
            df = pd.DataFrame()
            df['bytesReceived'] = epochs.groupby(['slot']).median()['bytesReceived'] * 1e-06
            df['Počet uzlov'] = numberOfNodes
            tmp.append(df)
        self.df = pd.concat(tmp, ignore_index=True)

    def analyze(self):
        plt.figure()
        g = sns.displot(hue='Počet uzlov', x='bytesReceived', data=self.df,
                        palette='colorblind', bins=60, kde=True,
                        height=3, aspect=2
                        )
        plt.xlabel('Uzlom prijaté dáta za slot [MB]')
        plt.ylabel('Počet')
        plt.tight_layout()
        for ax in g.axes.flatten():
            ax.set_xlim(0, 30)
        # plt.show()
        self.save_plot(f'solana-scenario03')
        stats = self.df.groupby(["networkSize"])["bytesReceived"]
        logger.logging.info(f'Min: {stats.min()}')
        logger.logging.info(f'Max: {stats.max()}')
        logger.logging.info(f'Median: {stats.median()}')

