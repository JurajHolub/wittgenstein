import json
import os
import statistics
import seaborn as sns
import pandas as pd
import requests
from matplotlib import pyplot as plt
from pymongo import MongoClient
import numpy as np

import logger
from scenarios.scenario import Scenario

class Scenario03(Scenario):
    """
    DDoS attack with VRF leader selection
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.epochDurationInSlots = 1000

    def simulate(self):
        network_size = 1500
        experiments_results = []
        for vrfEnabled, name in [(False, 'Rozvrh'), (True, 'VRF')]:
            for numberOfNodesUnderDos in [30]:
                parameters = {
                    "slotDurationInMs": 500,
                    "epochDurationInSlots": self.epochDurationInSlots,
                    "expectedTxPerBlock": 3000,
                    "networkSize": network_size,
                    "numberOfEpochs": 1,
                    "mongoServerAddress": self.mongoserver,
                    "p2pConnectionCount": 100,
                    "p2pMinimum": False,
                    "uniformStakeDistribution": True,
                    "txSizeInBytes": 670,
                    "blockHeaderSizeInBytes": 80,
                    "numberOfNodesUnderDos": numberOfNodesUnderDos,
                    "vrfLeaderSelection": vrfEnabled,
                }

                logger.logging.info(
                    f'Start simulate Ouroboros with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(self.ouroboros_endpoint, json=parameters)
                logger.logging.info(f'Simulation result: {response}')

                client = MongoClient()
                epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
                meadian_tps = epochs.groupby(['slot']).median()
                # slot_stack = epochs.groupby(pd.cut(epochs['slot'], self.epochDurationInSlots // 5))
                df = pd.DataFrame()
                df['Slot'] = meadian_tps.index
                df['TPS'] = meadian_tps['transactions']
                df['Počet uzlov pod DoS útokom'] = numberOfNodesUnderDos
                df['Voľba vodcu'] = name
                experiments_results.append(df)
        self.df = pd.concat(experiments_results, ignore_index=True)

    def analyze(self):
        plt.figure()
        sns.displot(self.df, x='TPS', hue='Voľba vodcu', multiple="dodge", bins=20)
        plt.ylabel('Počet')
        plt.xlabel('Tx/slot')
        plt.tight_layout()
        # plt.show()
        self.save_plot(f'ouroboros-scenario03')