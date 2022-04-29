import json

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
import requests
from pymongo import MongoClient
import seaborn as sns
from numpy import median

import logger
from scenarios.scenario import Scenario


class Scenario02(Scenario):
    """
    DoS attack comparison with and without VRF
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}

    def simulate(self):
        acum = []
        networkSize = 5000
        epochDurationInSlots = 1000
        tps = 3 * networkSize
        numberOfShards = networkSize // 250
        for numberOfNodesUnderDos in [50]:
            solana = {
                "slotDurationInMs": 400,
                "epochDurationInSlots": epochDurationInSlots,
                "validatorReliability": 100,
                "expectedTxPerBlock": int((tps / 1000) * 400),
                "networkSize": networkSize,
                "numberOfEpochs": 1,
                "uniformStakeDistribution": True,
                "mongoServerAddress": self.mongoserver,
                "vrfLeaderSelection": True,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "numberOfNodesUnderAttack": numberOfNodesUnderDos,
            }
            harmony = {
                "slotDurationInMs": 2000,
                "epochDurationInSlots": epochDurationInSlots,
                "numberOfEpochs": 1,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": networkSize,
                "numberOfShards": numberOfShards,
                "expectedTxPerBlock": int(((tps / 1000) * 2000) / numberOfShards),
                "byzantineNodes": 0,
                "lambda": 600,
                "ddosAttacks": True,
                "shardDoSMax": numberOfNodesUnderDos // numberOfShards,
                "mongoServerAddress": self.mongoserver,
                "uniformStakeDistribution": True,
                "vrfLeaderSelection": True,
            }
            ouroboros = {
                "slotDurationInMs": 1000,
                "epochDurationInSlots": epochDurationInSlots,
                "expectedTxPerBlock": int((tps / 1000) * 1000),
                "networkSize": networkSize,
                "numberOfEpochs": 1,
                "mongoServerAddress": self.mongoserver,
                "p2pConnectionCount": 100,
                "p2pMinimum": False,
                "uniformStakeDistribution": True,
                "txSizeInBytes": 670,
                "blockHeaderSizeInBytes": 80,
                "numberOfNodesUnderDos": numberOfNodesUnderDos,
                "vrfLeaderSelection": True,
            }
            protocols = [
                (solana, self.solana_endpoint, 'Solana'),
                (harmony, self.harmony_endpoint, 'Harmony'),
                (ouroboros, self.ouroboros_endpoint, 'Ouroboros')
            ]

            for parameters, endpoint, protocol in protocols:
                logger.logging.info(
                    f'Start simulate {protocol} with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(endpoint, json=parameters)
                logger.logging.info(f'Simulation of {protocol} result: {response}')

                client = MongoClient()
                epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
                if protocol == 'Harmony':
                    meadian_tps = epochs.groupby(['slot', 'shard']).median().groupby('slot').sum()
                else:
                    meadian_tps = epochs.groupby(['slot']).median()
                meadian_tps = meadian_tps['txCounterNonVote'] if protocol == 'Solana' else meadian_tps['transactions']
                df = pd.DataFrame()
                df['TPS'] = (meadian_tps / parameters['slotDurationInMs']) * 1000
                df['Protokol'] = protocol
                df['Počet uzlov pod DoS útokom'] = numberOfNodesUnderDos
                df['Slot'] = epochs['slot'].unique()
                df = self.z_score_normalisation(df, 'TPS', 'zscore')
                acum.append(df)
        self.df = pd.concat(acum, ignore_index=True)

    def analyze(self):
        plt.figure()
        ax = sns.barplot(data=self.df, x='Protokol', y='TPS', estimator=median, ci=None)
        medians = self.df.groupby(['Protokol', 'Počet uzlov pod DoS útokom']).median()['TPS']
        plt.ylim(medians.min() - 50, medians.max() + 50)
        [ax.bar_label(i, ) for i in ax.containers]
        # plt.show()
        self.save_plot(f'comparison-scenario02')

    def z_score_normalisation(self, df, column, new_col):
        # df[new_col] = (df[column] - df[column].mean()) / df[column].std()
        df[new_col] = (df[column] - df[column].min()) / (df[column].max() - df[column].min())
        return df

