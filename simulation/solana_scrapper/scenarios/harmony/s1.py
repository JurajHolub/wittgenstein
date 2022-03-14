import json
import os
import statistics
import seaborn as sns
import pandas as pd
import requests
from matplotlib import pyplot as plt

import logger
from scenarios.scenario import Scenario

class Scenario01(Scenario):
    """
    Sharding security:
    1000 nodes, 4 shards, various lambdas, uniform stake distribution
    plot heatmap of spearman correlation for each shard
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        self.spearman = pd.DataFrame()
        for token_lambda in [600, 700, 800, 900, 1000, 2000, 3000, 4000, 5000, 10_000, 20_000]:
            parameters = {
                "epochDurationInSlots": 50,
                "numberOfEpochs": 1,
                "vdfInSlots": 5,
                "txSizeInBytes": 32,
                "blockHeaderSizeInBytes": 80,
                "networkSize": 1000,
                "numberOfShards": 4,
                "expectedTxPerBlock": 500,
                "byzantineNodes": 0,
                "lambda": token_lambda,
            }
            logger.logging.info(f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')
            self.paths = json.loads(response.text)

            stake = self.open_epoch_df(self.paths['stake'][1][0])
            shard_node_count = []
            for i in range(4):
                stake.rename(columns={f'shardTokens.{i}': f'Shard {i}'}, inplace=True)
                shard_node_count.append((stake[f'Shard {i}'] != 0).sum())

            coeaf = stake.corr(method='spearman')[['stake']]
            coeaf = coeaf[coeaf.index.str.startswith('Shard')]
            self.spearman[f'λ={token_lambda}\n μ={statistics.mean(shard_node_count)}'] = coeaf

    def analyze(self):
        fig = plt.figure()
        sns.heatmap(self.spearman, vmin=-1, vmax=1)
        plt.xticks(rotation=70)
        fig.tight_layout()
        self.save_plot('shard-correlation')
