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
                "mongoServerAddress": self.mongoserver
            }

            logger.logging.info(
                f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.solana_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')

            client = MongoClient()
            leaders = pd.DataFrame(list(client.simulator.Leaders.find()))
            epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
            self.stats[f'{numberOfNodes} uzlov'] = epochs

            leader = []

            for index, row in leaders.sort_values(by='slot', ascending=True).iterrows():
                slot = epochs[epochs['slot'] == row['slot']]
                slot = slot[slot['node'] == row['leaderNode']]

                leader.append(slot['bytesReceived'].max() * 1e-06)

            self.leaders[f'{numberOfNodes} uzlov'] = [x for x in leader if not math.isnan(x)]

    def analyze(self):
        leaders_count = len(self.leaders.keys())
        fig = plt.figure()
        gs = gridspec.GridSpec(leaders_count, 1, height_ratios=[1 for i in range(leaders_count)])
        axs =[]
        for i in range(leaders_count):
            ax = fig.add_subplot(gs[i])
            #ax.tick_params(labelbottom=False)
            axs.append(ax)

        min_value = min([min(l) for l in self.leaders.values()])
        max_value = max([max(l) for l in self.leaders.values()])
        for (key, value), color, ax in zip(self.leaders.items(), sns.color_palette(), axs):
            logger.logging.info(f'Nodes: {key}, Throughput: {statistics.mean(value) / 400 * 1000}MB/sec')
            ax = sns.histplot(data=value,
                              stat="percent",
                              bins=20,
                              binrange=[min_value, max_value],
                              color=color,
                              ax=ax,
                              legend=True)
            ax.set(xlabel='', ylabel='Počet [%]', title=key)
        plt.xlabel('MB/slot')
        plt.tight_layout()
        #plt.show()
        self.save_plot(f'solana-scenario03')

