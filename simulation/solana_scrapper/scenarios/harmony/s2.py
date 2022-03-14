import json
import os
import re
import statistics
import seaborn as sns
import pandas as pd
import requests
from matplotlib import pyplot as plt

import logger
from scenarios.scenario import Scenario


class Scenario02(Scenario):
    """
    Sharding security:
    1000 nodes, 4 shards, 600 lambda, uniform stake distribution, various byzantine nodes
    plot heatmap of spearman correlation for each shard
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        self.byzantine = {}
        for byzantine_nodes in [200, 220, 240, 260, 280, 300, 320, 340]:
            parameters = {
                "epochDurationInSlots": 50,
                "numberOfEpochs": 1000,
                "vdfInSlots": 5,
                "txSizeInBytes": 32,
                "blockHeaderSizeInBytes": 80,
                "networkSize": 1000,
                "numberOfShards": 4,
                "expectedTxPerBlock": 500,
                "byzantineNodes": byzantine_nodes,
                "lambda": 600,
            }
            logger.logging.info(
                f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')
            self.paths = json.loads(response.text)

            attacks = {}
            for epoch in self.paths['stake'][1]:
                df = self.open_epoch_df(epoch)
                epoch_num = re.findall('(\\d+)', epoch)[-1]
                shard = df.loc[:, df.columns.str.startswith('shardTokens')]
                for i in range(len(shard.columns)):
                    shard_nodes = (shard[f'shardTokens.{i}'] != 0)
                    shard_stake = df[shard_nodes]['tokens'].sum()
                    shard_byzantine = df[shard_nodes][df[shard_nodes]['byzantine']]['tokens'].sum()
                    attacks.setdefault(f'Hlasovací podiel', []).append(shard_byzantine / shard_stake * 100)
                    attacks.setdefault(f'Shard', []).append(i)
                    attacks.setdefault(f'Epocha', []).append(epoch_num)
            self.byzantine[byzantine_nodes] = pd.DataFrame(attacks)

    def analyze(self):
        for total_byzantine, df in self.byzantine.items():
            fig = plt.figure()
            ax = sns.histplot(df, x='Hlasovací podiel')
            for p in ax.patches:
                if p.get_x() >= 33:
                    p.set_facecolor('r')
            ax.set(xlabel='Hlasovací podiel byzantských uzlov v shardoch [%]', ylabel='Počet')
            successful_overpower = f'{df[df["Hlasovací podiel"] > 33]["Hlasovací podiel"].count()} incidentov'
            ax.text(0.7, 0.5, successful_overpower,
                    horizontalalignment='left',
                    verticalalignment='center',
                    transform=ax.transAxes)
            fig.tight_layout()
            self.save_plot(f'byzantine{total_byzantine}-shard-kde')
