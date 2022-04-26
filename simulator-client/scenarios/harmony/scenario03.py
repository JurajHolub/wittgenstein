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


class Scenario03(Scenario):
    """
    Sharding throughput:
    1000 nodes, 4 shards, various lambdas
    plot Tx/slot, Tx/sec
    plot MB/slot, MB/sec
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}

    def simulate(self):
        slots = 200
        token_lambda = 600
        for shards in range(2, 41, 2):
            nodes = 250 * shards
            expectedTxPerBlock = 500 * shards
            parameters = {
                "slotDurationInMs": 2000,
                "epochDurationInSlots": slots,
                "numberOfEpochs": 1,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": nodes,
                "numberOfShards": shards,
                "expectedTxPerBlock": expectedTxPerBlock,
                "byzantineNodes": 0,
                "lambda": token_lambda,
                "ddosAttack": False,
                "mongoServerAddress": self.mongoserver,
                "uniformStakeDistribution": True
            }
            logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')
            db = self.get_data_from_mongo()

            result = db.Epochs.aggregate([
                {
                    '$group': {
                        '_id': None,
                        'received': {
                            '$avg': '$bytesReceived'
                        },
                        'sent': {
                            '$avg': '$bytesSent'
                        },
                        'transactions': {
                            '$avg': '$transactions'
                        }
                    }
                }
            ]).next()

            self.stats.setdefault('receivedMbPerNode', []).append(result['received'] * 1e-06)
            self.stats.setdefault('sentMbPerNode', []).append(result['sent'] * 1e-06)
            self.stats.setdefault('tpb', []).append(result['transactions'])
            self.stats.setdefault('shards', []).append(shards)

    def bytes_to_mbps(self, bytes, slots, epochs):
        return (bytes / (slots * epochs)) * 1e-06

    def analyze(self):
        logger.logging.info(f'Plot graph')
        fig = plt.figure()
        gs = gridspec.GridSpec(3, 1, height_ratios=[1, 1, 1])
        ax0 = fig.add_subplot(gs[0])
        ax1 = fig.add_subplot(gs[1])
        ax2 = fig.add_subplot(gs[2])
        labels = [str(shards) for shards in self.stats['shards']]

        pd.DataFrame({
            'Počet uzlov': [250*shards for shards in self.stats['shards']],
        }, index=labels).plot.bar(ax=ax0, legend=False, rot=0)
        ax0.set_ylabel('Počet uzlov')
        ax0.set_xlabel('Počet shardov')
        ax0.grid(axis="y", linestyle='--')

        pd.DataFrame({
            'Tx/slot': self.stats['tpb'],
        }, index=labels).plot.bar(ax=ax1, legend=False, rot=0)
        ax1.set_ylabel('Tx/slot')
        ax1.set_xlabel('Počet shardov')
        ax1.grid(axis="y", linestyle='--')

        pd.DataFrame({
            'MB/slot': self.stats['receivedMbPerNode'],
        }, index=labels).plot.bar(ax=ax2, legend=False, rot=0, color='r')
        ax2.set_ylabel('MB/slot')
        ax2.set_xlabel('Počet shardov')
        ax2.grid(axis="y", linestyle='--')

        fig.tight_layout()
        self.save_plot(f'harmony-scenario03')
