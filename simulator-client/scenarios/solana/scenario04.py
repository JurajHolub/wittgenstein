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
        self.epochDurationInSlots = 10000

    def simulate(self):
        experiments_results = []
        for vrfEnabled, name in [(False, 'Rozvrh'), (True, 'VRF')]:
            stats = {}
            for ddos_nodes in [5]:
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
                }

                logger.logging.info(
                    f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(self.solana_endpoint, json=parameters)
                logger.logging.info(f'Simulation result: {response}')

                client = MongoClient()
                epochs = pd.DataFrame(list(client.simulator.Epochs.find()))

                slot_stack = epochs.groupby(pd.cut(epochs['slot'], self.epochDurationInSlots // 5))
                df = pd.DataFrame()
                df['Slot'] = slot_stack.max()['slot']
                df['TPS'] = slot_stack.mean()['txCounterNonVote']
                df['Počet uzlov pod DoS útokom'] = ddos_nodes
                df['Voľba vodcu'] = name
                experiments_results.append(df)
        self.df = pd.concat(experiments_results, ignore_index=True)

    def analyze(self):
        plt.figure()
        g = sns.FacetGrid(self.df, row='Počet uzlov pod DoS útokom', palette='colorblind', legend_out=False, aspect=4)
        g.map(sns.lineplot, 'Slot', 'TPS', 'Voľba vodcu')
        g.add_legend()
        plt.tight_layout()
        # plt.show()
        self.save_plot(f'solana-scenario04')
