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
        self.stats = {
            'tpb': [],
            'mbPerNode': [],
            'shards': [],
        }

    def simulate(self):
        slots = 300
        token_lambda = 600
        num_of_epochs = 3
        for shards in range(2, 21):
            nodes = 250 * shards
            parameters = {
                "epochDurationInSlots": slots,
                "numberOfEpochs": num_of_epochs,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": nodes,
                "numberOfShards": shards,
                "expectedTxPerBlock": 500,
                "byzantineNodes": 0,
                "lambda": token_lambda,
                "ddosAttack": False,
            }
            logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')
            db = self.get_data_from_mongo()
            leaders = pd.DataFrame(list(db.Leaders.find()))

            tpb_acum = []
            bytesReceived = 0
            #bytesSent = 0
            for epoch_num in range(num_of_epochs):
                logger.logging.info(f'Parse epoch {epoch_num}')
                df = pd.DataFrame(list(db.Epochs.find({'epoch': epoch_num})))
                tpb = 0
                for i, leader in leaders[leaders['epoch'] == epoch_num].iterrows():
                    tpb += df[df['node'] == leader['node']][df['shard'] == leader['shard']]['transactions'].sum()
                df = df.groupby(by=['node'], as_index=False).max()
                bytesReceived += df['bytesReceived'].sum()
                tpb_acum.append(tpb)
            self.stats['mbPerNode'].append(bytesReceived / (nodes * num_of_epochs) * 1e-06)
            self.stats['tpb'].append(sum(tpb_acum) / (len(tpb_acum) * slots))
            self.stats['shards'].append(shards)

    def bytes_to_mbps(self, bytes, slots, epochs):
        return (bytes / (slots * epochs)) * 1e-06

    def analyze(self):
        logger.logging.info(f'Plot graph')
        fig = plt.figure()
        gs = gridspec.GridSpec(3, 1, height_ratios=[1, 1, 1])
        ax0 = fig.add_subplot(gs[0])
        ax0.tick_params(labelbottom=False)
        ax1 = fig.add_subplot(gs[1])
        ax1.tick_params(labelbottom=False)
        ax2 = fig.add_subplot(gs[2])
        labels = [str(shards) for shards in self.stats['shards']]
        pd.DataFrame({
            'MB/slot': self.stats['mbPerNode'],
        }, index=labels).plot.bar(ax=ax0, legend=False)
        ax0.set_ylabel('MB/slot')
        ax0.grid(axis="y", linestyle='--')
        pd.DataFrame({
            'Tx/slot': self.stats['tpb'],
        }, index=labels).plot.bar(ax=ax1, legend=False)
        ax1.set_ylabel('Tx/slot')
        ax1.grid(axis="y", linestyle='--')
        pd.DataFrame({
            'Počet uzlov': [250*shards for shards in self.stats['shards']],
        }, index=labels).plot.bar(ax=ax2, legend=False)
        ax2.set_ylabel('Počet uzlov')
        ax2.set_xlabel('Počet shard')
        ax2.grid(axis="y", linestyle='--')
        plt.xticks(rotation=90)
        fig.tight_layout()
        self.save_plot(f'tps-stats')
