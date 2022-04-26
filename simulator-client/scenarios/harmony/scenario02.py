import json
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
    plot histogram of voting stake for 1000 epoch
    """

    def __init__(self, output_path):
        super().__init__(output_path)

    def simulate(self):
        self.byzantine = {}
        num_of_epochs = 1000
        for byzantine_share in [0.28, 0.30, 0.32]:
            parameters = {
                "slotDurationInMs": 2000,
                "epochDurationInSlots": 50,
                "numberOfEpochs": num_of_epochs,
                "vdfInSlots": 5,
                "txSizeInBytes": 32,
                "blockHeaderSizeInBytes": 80,
                "networkSize": 1000,
                "numberOfShards": 4,
                "expectedTxPerBlock": 500,
                "byzantineShare": byzantine_share,
                "lambda": 600,
                "ddosAttack": False,
                "mongoServerAddress": self.mongoserver,
                "uniformStakeDistribution": True
            }
            logger.logging.info(
                f'Start simulate Harmony with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
            response = requests.post(self.harmony_endpoint, json=parameters)
            logger.logging.info(f'Simulation result: {response}')

            db = self.get_data_from_mongo()
            attacks = {}
            for epoch_num in range(num_of_epochs):
                logger.logging.info(f'Parse epoch {epoch_num}')
                data = list(db.StakeStats.find({'epoch': epoch_num}))
                df = pd.json_normalize(data)
                shard = df.loc[:, df.columns.str.startswith('shardTokens')]
                for i in range(len(shard.columns)):
                    shard_nodes = (shard[f'shardTokens.{i}'] != 0)
                    shard_stake = df[shard_nodes]['tokens'].sum()
                    shard_byzantine = df[shard_nodes][df[shard_nodes]['byzantine']]['tokens'].sum()
                    attacks.setdefault(f'Hlasovací podiel', []).append(shard_byzantine / shard_stake * 100)
                    attacks.setdefault(f'Shard', []).append(i)
                    attacks.setdefault(f'Epocha', []).append(epoch_num)
            self.byzantine[f'byzantine{byzantine_share}'] = pd.DataFrame(attacks)

    def analyze(self):
        for byzantine_share, df in self.byzantine.items():
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
            self.save_plot(f'harmony-scenario02--{byzantine_share}')
