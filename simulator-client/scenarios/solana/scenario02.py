import json

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
import requests
from pymongo import MongoClient

import logger
from scenarios.scenario import Scenario


class Scenario02(Scenario):
    """
    Simulate X epochs with 1500 nodes (same as real network). The nodes stake is taken from the real network.
    Set up TPS to the real one (around 3k).
    Re-simulate with ddos-attack. Intruders attacks N richest nodes (10, 15, 20).
    Check the TPS drop according to the size of ddos attack.
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}

    def simulate(self):
        stats = {
            True: {},  # uniform stake distribution
            False: {},  # the real one
        }
        for stakeDist in [False, True]:
            for ddos_nodes in range(0, 51, 5):
                parameters = {
                    "slotDurationInMs": 400,
                    "epochDurationInSlots": 1000,
                    "validatorReliability": 100,
                    "expectedTxPerBlock": 1500,
                    "networkSize": 1500,
                    "numberOfEpochs": 1,
                    "numberOfNodesUnderAttack": ddos_nodes,
                    "uniformStakeDistribution": stakeDist,
                    "mongoServerAddress": self.mongoserver
                }

                logger.logging.info(
                    f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(self.solana_endpoint, json=parameters)
                logger.logging.info(f'Simulation result: {response}')

                client = MongoClient()
                stake = pd.DataFrame(list(client.simulator.Stake.find()))
                leaders = pd.DataFrame(list(client.simulator.Leaders.find()))
                tps = list(client.simulator.Epochs.aggregate([{
                    '$group': {
                        '_id': None,
                        'mean': {
                            '$avg': '$txCounterNonVote'
                        }
                    }
                }]))[0]['mean']
                under_ddos = stake.loc[stake['node'].isin(leaders[leaders['underDdos']]['leaderNode'])]
                stats[stakeDist].setdefault('ddosed-stake', []).append(under_ddos['stake'].sum() / stake['stake'].sum() *100)
                stats[stakeDist].setdefault('tps', []).append(tps)
                stats[stakeDist].setdefault('ddos-nodes', []).append(ddos_nodes)
            self.stats[stakeDist] = pd.DataFrame(stats[stakeDist])
            self.stats[stakeDist].set_index('ddos-nodes', inplace=True)

    def analyze(self):
        fig = plt.figure()
        gs = gridspec.GridSpec(2, 1, height_ratios=[1, 1])
        ax0 = fig.add_subplot(gs[0])
        ax0.tick_params(labelbottom=False)
        ax1 = fig.add_subplot(gs[1])
        ax0.set_ylabel("TPS")
        pd.DataFrame({
            'Rovnomerné rozdelenie': self.stats[True]['tps'],
            'Skutočné rozdelenie': self.stats[False]['tps'],
        }, index=self.stats[False].index).plot.line(rot=45, ax=ax0)
        pd.DataFrame({
            'Rovnomerné rozdelenie': self.stats[True]['ddosed-stake'],
            'Skutočné rozdelenie': self.stats[False]['ddosed-stake']
        }, index=self.stats[False].index).plot.line(rot=45, ax=ax1)
        ax1.set_ylabel("Hlasovací podiel")
        ax0.set_xlabel('')
        ax1.set_xlabel('Počet uzlov pod DDoS útokom')
        ax1.yaxis.set_major_formatter(mtick.PercentFormatter())
        ax0.grid(axis="y", linestyle='--')
        ax1.grid(axis="y", linestyle='--')
        plt.tight_layout()
        #plt.show()
        self.save_plot(f'solana-scenario02')
