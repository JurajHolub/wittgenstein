import json

import pandas as pd
import matplotlib.pyplot as plt
import requests
from pymongo import MongoClient
import seaborn as sns

import logger
from scenarios.scenario import Scenario


class Scenario01(Scenario):
    """
    Simulate X epochs with 100, 200, .., 1500, ... nodes.
    Simulate with various TPS configuration: 710k (maximum), 3k (real), 50k (faster than Visa).
    Show ratio of voting / nonvoting transactions in TPS.
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        acum = []
        for networkSize in [1000, 10_000]:
            for expectedTps in [1000, 10_000, 20_000, 50_000]:
                parameters = {
                    "slotDurationInMs": 400,
                    "epochDurationInSlots": 200,
                    "validatorReliability": 0.95,
                    "expectedTxPerBlock": expectedTps,
                    "networkSize": networkSize,
                    "numberOfEpochs": 1,
                    "mongoServerAddress": self.mongoserver,
                    "numberOfNodesUnderAttack": 0,
                    "uniformStakeDistribution": True,
                    "txSizeInBytes": 670,  # see bitcoin-block-size.py
                }

                logger.logging.info(
                    f'Start simulate Solana with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(self.solana_endpoint, json=parameters)
                logger.logging.info(f'Simulation result: {response}')

                client = MongoClient()
                df = pd.DataFrame(list(client.simulator.Epochs.find()))

                tpb = pd.DataFrame({
                    'nonvote': df.groupby('slot').mean()['txCounterNonVote'],
                    'vote': df.groupby('slot').mean()['txCounterVote'],
                    'slot': df['slot'].unique()
                })
                tpb['TPS'] = expectedTps / 400 * 1000
                tpb['Počet uzlov'] = networkSize
                acum.append(tpb)

        self.tpb = pd.concat(acum, ignore_index=True)

    def analyze(self):
        fig = plt.figure()
        g = sns.FacetGrid(self.tpb, col='TPS', row='Počet uzlov', legend_out=False)
        g = g.map_dataframe(self.ploter)
        g.add_legend()
        plt.tight_layout()
        #plt.show()
        self.save_plot('solana-scenario01')

        logger.logging.info(f'Ratio of nonvote/vote transactions:')
        logger.logging.info((self.tpb['nonvote'] / self.tpb['vote']).describe())

    def ploter(self, data, color):
        nonvote = data['nonvote'] / (data['nonvote'] + data['vote']) * 100
        vote = data['vote'] / (data['nonvote'] + data['vote']) * 100
        plt.stackplot(data['slot'],
                      nonvote,
                      vote,
                      labels=['Aplikačné transakcie', 'Hlasovacie transakcie']
                      )
        logger.logging.info(f'TPS={data["TPS"].max()},networkSize={data["Počet uzlov"].max()}:')
        logger.logging.info(f'nonvote={nonvote.mean()}, vote={vote.mean()}')
