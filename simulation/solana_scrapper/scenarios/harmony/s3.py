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

    def simulate(self):
        df_msg = []
        nodes = 1000
        slots = 1000
        token_lambda = 600
        for shards in range(2, 30):
            nodes = 250 * shards
            parameters = {
                "epochDurationInSlots": slots,
                "numberOfEpochs": 3,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": nodes,
                "numberOfShards": shards,
                "expectedTxPerBlock": 600,
                "byzantineNodes": 0,
                "lambda": token_lambda,
            }
            logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')
            self.paths = json.loads(response.text)
            leaders = pd.read_csv(self.paths['leaders'])

            leaders_acum = []
            non_leaders_acum = []
            total_acum = []
            tpb_acum = []
            for epoch in self.paths['epochs'][1]:
                epoch_num = int(re.findall('(\\d+)', epoch)[-1])
                df = pd.read_csv(epoch)
                tpb = 0
                for _, leader in leaders[leaders['epoch'] == epoch_num].iterrows():
                    tpb += df[df['node'] == leader['node']][df['shard'] == leader['shard']]['transactions'].sum()
                df = df.groupby(by=['node'], as_index=False).max()
                epoch_leaders = leaders[leaders['epoch'] == epoch_num]
                epoch_leaders = df[df['node'].isin(list(epoch_leaders['node']))]
                epoch_non_leaders = df[~df['node'].isin(list(epoch_leaders['node']))]
                leaders_acum.append(epoch_leaders)
                non_leaders_acum.append(epoch_non_leaders)
                total_acum.append(df)
                tpb_acum.append(tpb)

            epoch_leaders = pd.concat(leaders_acum, ignore_index=True)
            epoch_non_leaders = pd.concat(non_leaders_acum, ignore_index=True)
            total = pd.concat(total_acum, ignore_index=True)
            df_msg.append({
                'shards': shards,
                'tpb': sum(tpb_acum) / (len(tpb_acum) * slots),
                'bytes_sent': self.bytes_to_mbps(total['bytesReceived'].sum(), slots),
                'bytes_received': self.bytes_to_mbps(total['bytesSent'].sum(), slots),
                'total_sent': self.bytes_to_mbps(total['msgReceived'].sum(), slots),
                'total_received': self.bytes_to_mbps(total['msgSent'].sum(), slots),
                'leader_received': self.bytes_to_mbps(epoch_leaders['msgReceived'].mean(), slots),
                'leader_sent': self.bytes_to_mbps(epoch_leaders['msgSent'].mean(), slots),
                'non_leader_received': self.bytes_to_mbps(epoch_non_leaders['msgReceived'].mean(), slots),
                'non_leader_sent': self.bytes_to_mbps(epoch_non_leaders['msgSent'].mean(), slots),
                'nodes': nodes
            })
        self.df_msg = pd.DataFrame(df_msg)
        pass

    def bytes_to_mbps(self, bytes, slots):
        return (bytes / slots) * 1e-06

    def analyze(self):
        fig = plt.figure()
        gs = gridspec.GridSpec(3, 1, height_ratios=[1, 1, 1])
        ax0 = fig.add_subplot(gs[0])
        ax0.tick_params(labelbottom=False)
        ax1 = fig.add_subplot(gs[1])
        ax1.tick_params(labelbottom=False)
        ax2 = fig.add_subplot(gs[2])
        labels = [str(shards) for shards in list(self.df_msg['shards'])]
        pd.DataFrame({
            'MB/slot': list(self.df_msg['bytes_sent'] / self.df_msg['nodes']),
        }, index=labels).plot.line(ax=ax0, legend=False, style='.-')
        ax0.set_ylabel('MB/slot')
        ax0.grid(axis="y", linestyle='--')
        pd.DataFrame({
            'Tx/slot': list(self.df_msg['tpb']),
        }, index=labels).plot.line(ax=ax1, legend=False, style='.-')
        ax1.set_ylabel('Tx/slot')
        ax1.grid(axis="y", linestyle='--')
        pd.DataFrame({
            'Počet uzlov': [250*shards for shards in list(self.df_msg['shards'])],
        }, index=labels).plot.bar(ax=ax2, legend=False)
        ax2.set_ylabel('Počet uzlov')
        ax2.set_xlabel('Počet shard')
        ax2.grid(axis="y", linestyle='--')
        plt.xticks(rotation=90)
        fig.tight_layout()
        self.save_plot(f'tps-stats')
