import json

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
import requests
from pymongo import MongoClient
import seaborn as sns

import logger
from scenarios.scenario import Scenario


class Scenario04(Scenario):
    """
    Leader DDoS attack with VRF feature.
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}
        self.epochDurationInSlots = 10_000

    def simulate(self):
        experiments_results = []
        ddos_nodes = 30
        for vrfEnabled, name in [(False, 'Rozvrh'), (True, 'VRF')]:
            parameters = {
                "slotDurationInMs": 400,
                "epochDurationInSlots": self.epochDurationInSlots,
                "validatorReliability": 100,
                "expectedTxPerBlock": 1500,
                "networkSize": 100,
                "numberOfEpochs": 1,
                "numberOfNodesUnderAttack": ddos_nodes,
                "uniformStakeDistribution": True,
                "mongoServerAddress": self.mongoserver,
                "vrfLeaderSelection": vrfEnabled,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
            }

            logger.logging.info(
                f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.solana_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')

            client = MongoClient()
            epochs = pd.DataFrame(list(client.simulator.Epochs.find()))

            slot_stack = epochs.groupby(pd.cut(epochs['slot'], self.epochDurationInSlots // 10))
            df = pd.DataFrame()
            df['Slot'] = slot_stack.max()['slot']
            df['TPS'] = slot_stack.mean()['txCounterNonVote']
            df['Počet uzlov pod DoS útokom'] = ddos_nodes
            df['Voľba vodcu'] = name
            experiments_results.append(df)
        self.df = pd.concat(experiments_results, ignore_index=True)

    def analyze(self):
        plt.figure()
        #g = sns.FacetGrid(self.df.iloc[2000:3000], row='Počet uzlov pod DoS útokom', palette='colorblind', legend_out=False, aspect=4)
        #df = self.df[self.df['Slot'].isin([*range(1000, 3000)])]
        sns.displot(self.df, x='TPS', hue='Voľba vodcu', kind='kde', fill=True)
        plt.ylabel('Odhad hustoty')
        #g.map(sns.lineplot, 'Slot', y='TPS', hue='Voľba vodcu')
        #g.add_legend()
        plt.tight_layout()
        # plt.show()
        self.save_plot(f'solana-scenario04')
