import pandas as pd
import SimulatorSubproces as java

from wsparser.SolanaParser import SolanaParser as out
import matplotlib.pyplot as plt


class Scenario01:
    """
    Simulate X epochs with 100, 200, .., 1500, ... nodes.
    Simulate with various TPS configuration: 710k (maximum), 3k (real), 50k (faster than Visa).
    Show ratio of voting / nonvoting transactions in TPS.
    """

    def __init__(self):
        self.stats = pd.DataFrame()

    def calculate_epoch_stats(self, epoch, epoch_df, df_stake, df_leader):
        self.stats = self.stats.append({
            'epoch': epoch,
            'epoch_end_time': epoch_df['arriveTime'].mean(),
            'tps_vote': epoch_df['txCounterVote'].sum(),
            'tps_non_vote': epoch_df['txCounterNonVote'].sum(),
            'tps_total': (epoch_df['txCounterVote'] + epoch_df['txCounterNonVote']).sum(),
        }, ignore_index=True)

    def simulate(self):
        java.SimulatorBuild()
        for tps in [100_000]:
            mean_tps_vote = []
            mean_tps_non_vote = []
            labels = []
            for nodes in [100, 500, 1000, 1500, 2000, 4000, 10_000, 20_000]:
                scenario01 = Scenario01()
                p = java.SimulatorSubprocess(protocol='solana', nodes=nodes, epoch=5, tps=tps).run()
                out.Parser().parse(scenario01.calculate_epoch_stats)
                mean_tps_vote.append(scenario01.stats['tps_vote'].sum() / scenario01.stats['epoch_end_time'].max() * 1000)
                mean_tps_non_vote.append(scenario01.stats['tps_non_vote'].sum() / scenario01.stats['epoch_end_time'].max() * 1000)
                labels.append(nodes)

            df = pd.DataFrame({
                'Aplikačné transakcie': mean_tps_non_vote,
                'Hlasovacie transakcie': mean_tps_vote,
            }, index=labels)
            ax = df.plot(kind='bar', stacked=True, rot=45)
            ax.grid(axis="y", linestyle='--')
            ax.set_xlabel('Počet uzlov', fontsize=10)
            ax.set_ylabel('TPS', fontsize=10)
            plt.tight_layout()
            plt.show()
