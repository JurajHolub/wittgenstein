import json

import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
import requests
from pymongo import MongoClient
import seaborn as sns

import logger
from scenarios.scenario import Scenario


class Scenario01(Scenario):
    """
    Data usage comparison.
    """

    def __init__(self, output_path):
        super().__init__(output_path)
        self.stats = {}

    def simulate(self):
        acum = []
        for networkSize in [1000, 5000, 10_000]:
            self.epochDurationInSlots = 300
            tps = 3 * networkSize
            numberOfShards = networkSize // 250
            solana = {
                "slotDurationInMs": 400,
                "epochDurationInSlots": self.epochDurationInSlots,
                "validatorReliability": 100,
                "expectedTxPerBlock": int((tps / 1000) * 400),
                "networkSize": networkSize,
                "numberOfEpochs": 1,
                "numberOfNodesUnderAttack": 0,
                "uniformStakeDistribution": True,
                "mongoServerAddress": self.mongoserver,
                "vrfLeaderSelection": False,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
            }
            harmony = {
                "slotDurationInMs": 2000,
                "epochDurationInSlots": self.epochDurationInSlots,
                "numberOfEpochs": 1,
                "vdfInSlots": 5,
                "txSizeInBytes": 670,  # see bitcoin-block-size.py
                "blockHeaderSizeInBytes": 80,
                "networkSize": networkSize,
                "numberOfShards": numberOfShards,
                "expectedTxPerBlock": int(((tps / 1000) * 2000) / numberOfShards),
                "byzantineNodes": 0,
                "lambda": 600,
                "ddosAttacks": False,
                "mongoServerAddress": self.mongoserver,
                "uniformStakeDistribution": True,
                "vrfLeaderSelection": False,
            }
            protocols = [
                (solana, self.solana_endpoint, 'Solana'),
                (harmony, self.harmony_endpoint, 'Harmony')
            ]

            for parameters, endpoint, protocol in protocols:
                logger.logging.info(
                    f'Start simulate {protocol} with parameters: {json.dumps(parameters, sort_keys=False, indent=4)}')
                response = requests.post(endpoint, json=parameters)
                logger.logging.info(f'Simulation of {protocol} result: {response}')

                client = MongoClient()
                epochs = pd.DataFrame(list(client.simulator.Epochs.find()))
                df = pd.DataFrame()
                #bytesReceived = epochs.groupby(['node', 'slot'])['bytesReceived'].sum()

                if protocol == 'Harmony':
                    bytesReceived = epochs.groupby(['slot', 'shard']).median().groupby('slot').sum()['bytesReceived']
                else:
                    bytesReceived = epochs.groupby(['slot']).median()['bytesReceived']

                df['megaBytesPerSecond'] = ((bytesReceived * 1e-06) / parameters['slotDurationInMs']) * 1000
                df['megaBytesPerSlot'] = bytesReceived * 1e-06
                df['Protokol'] = protocol
                df['Počet uzlov'] = networkSize
                acum.append(df)
        self.df = pd.concat(acum, ignore_index=True)

    def analyze(self):
        plt.figure()
        g = sns.FacetGrid(self.df, hue='Počet uzlov', palette='colorblind', row='Protokol',
                          legend_out=False, aspect=3
                          )
        g.map(sns.histplot, 'megaBytesPerSecond', linewidth=0)
        g.add_legend()
        # repeating x axis labels for all facets
        for ax in g.axes.flatten():
            ax.tick_params(labelbottom=True)
            ax.set_xlabel('MB/s')
            ax.set_ylabel('Počet')
            ax.set_xlim(0, 35) # remove outliers
        plt.tight_layout()
        #plt.show()
        self.save_plot(f'comparison-scenario01')
        pd.set_option("display.max_rows", None, "display.max_columns", None)
        self.df.groupby(['Protokol', 'Počet uzlov'])['megaBytesPerSecond'].describe()
        """
                                          count       mean       std        min        25%  \
            Protokol Počet uzlov                                                     
            Harmony  1000         300.0   1.507802  0.089332   1.269827   1.449555   
                     5000         300.0   8.388182  0.219171   7.803562   8.246725   
                     10000        300.0  16.772572  0.308672  15.860384  16.585491   
            Solana   1000         299.0   2.697611  0.280172   0.640000   2.540615   
                     5000         299.0  13.477988  1.139870  10.868990  12.713165   
                     10000        299.0  27.102526  2.344653  20.934815  25.671715   
                                        50%        75%        max  
            Protokol Počet uzlov                                   
            Harmony  1000          1.509520   1.567726   1.731122  
                     5000          8.395004   8.528502   9.005877  
                     10000        16.753661  16.968660  17.658161  
            Solana   1000          2.691040   2.841790   4.611430  
                     5000         13.408015  14.129587  24.003505  
                     10000        26.860465  28.337477  49.000280  
        """

