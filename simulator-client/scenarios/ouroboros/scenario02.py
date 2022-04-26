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

class Scenario02(Scenario):
    """
    Forking
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        self.network_size = 1500
        self.epochDurationInSlots = 1000
        parameters = {
            "slotDurationInMs": 1000,
            "epochDurationInSlots": self.epochDurationInSlots,
            "expectedTxPerBlock": 3000,
            "networkSize": self.network_size,
            "numberOfEpochs": 1,
            "mongoServerAddress": self.mongoserver,
            "p2pConnectionCount": 50,
            "p2pMinimum": False,
            "uniformStakeDistribution": False,
            "txSizeInBytes": 670,
            "blockHeaderSizeInBytes": 80,
            "numberOfNodesUnderDos": 0,
            "vrfLeaderSelection": False,
            "byzantineStake": 0.15,
            "forkRatio": 5,
        }

        logger.logging.info(
            f'Start simulate Ouroboros with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
        response = requests.post(self.ouroboros_endpoint, json=parameters)
        logger.logging.info(f'Simulation result: {response}')

        client = MongoClient()

        leaders = pd.DataFrame(list(client.simulator.Leaders.find()))
        byzantine_leaders = leaders[leaders['byzantine']]
        byzantine_leaders = byzantine_leaders.sort_values('slot')
        self.ranges = self.find_continuous_number_ranges(np.array(byzantine_leaders['slot'].tolist()))

        stake = pd.DataFrame(list(client.simulator.Stake.find()))
        self.byzantine_stake = stake[stake['byzantine']]['stake'].sum() / stake['stake'].sum()
        self.byzantine_nodes = stake[stake['byzantine']]['node'].count()

        epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
        self.forks = {}
        for r in self.ranges:
            for slot in range(r[0], r[1]+1):
                fork = epochs[epochs['slot'] == slot]['hash'].value_counts()
                fork.reset_index()
                self.forks[slot] = fork

        max_fork = max([f.count() for f in self.forks.values()])
        self.area = {}
        for slot in range(self.epochDurationInSlots):
            if slot in self.forks:
                f = self.forks[slot]
                for i in range(max_fork):
                    self.area.setdefault(i, []).append(f.iloc[i] if i < f.count() else 0)
            else:
                self.area.setdefault(0, []).append(self.network_size)
                for i in range(1, max_fork):
                    self.area.setdefault(i, []).append(0)

    def analyze(self):
        fig = plt.figure()
        incidents = pd.DataFrame([r[1]+1 - r[0] for r in self.ranges]).value_counts()
        df = pd.DataFrame({
            'incidents': incidents.values
        }, index=[k[0] for k in incidents.keys()])
        plots = sns.barplot(x=df.index, y=df.incidents)
        [plots.bar_label(i, ) for i in plots.containers]
        plt.xlabel('Počet kontinuálnych byzantských blokov')
        plt.ylabel('Počet výskytov')
        plt.tight_layout()
        # plt.show()
        self.save_plot(f'ouroboros-scenario02-hist')

        plt.figure()
        df = pd.DataFrame(self.area, index=range(self.epochDurationInSlots))
        df.rename(columns=lambda fork: f'Fork {fork}', inplace=True)
        df.plot.area(stacked=True)
        plt.xlabel('Slot')
        plt.ylabel('Počet uzlov')
        plt.tight_layout()
        #plt.show()
        self.save_plot(f'ouroboros-scenario02-area')

        plt.figure()
        byzantine_slots = df[df['Fork 0'] != self.network_size]
        byzantine_slots = byzantine_slots[byzantine_slots['Fork 0'] < self.network_size//2]  # filter only majority
        logger.logging.info(f'Majority incident: {byzantine_slots.count()}')
        byzantine_slots = byzantine_slots.reset_index(drop=True)
        ax = byzantine_slots.plot.bar(stacked=True, width=0.8)
        ax.tick_params(labelbottom=False)
        plt.axhline(y=self.network_size//2, color='r', linestyle='-')
        plt.xlabel('Fork incident')
        plt.ylabel('Počet uzlov')
        plt.tight_layout()
        #plt.show()
        self.save_plot(f'ouroboros-scenario02-majority')

        logger.logging.info(f'Byzantský podiel: {self.byzantine_stake}')
        logger.logging.info(f'Poctivý podiel: {1 - self.byzantine_stake}')
        logger.logging.info(f'Byzantské uzly: {self.byzantine_nodes}')
        logger.logging.info(f'Poctivé uzly: {self.network_size - self.byzantine_nodes}')

    def find_continuous_number_ranges(self, data):
        """
        https://stackoverflow.com/questions/2154249/identify-groups-of-continuous-numbers-in-a-list
        """
        d = [i for i, df in enumerate(np.diff(data)) if df != 1]
        d = np.hstack([-1, d, len(data) - 1])  # add first and last elements
        d = np.vstack([d[:-1] + 1, d[1:]]).T
        return data[d]