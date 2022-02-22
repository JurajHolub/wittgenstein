import pandas as pd
import SimulatorSubproces as java
import OutputMetadata as out
import matplotlib.pyplot as plt

class Scenario03:
    """
    Simulate X epochs with 1500 nodes (same as real network). The nodes stake is taken from the real network.
    Check how many SOL earns leaders from transaction fees.
    Re-simulate with ddos-attack. Intruders attacks N richest nodes (10, 15, 20).
    Che
    """

    def __init__(self, nodes):
        self.LAMPORT = 1
        self.SOL = 1_000_000_000 * self.LAMPORT
        self.TX_FEE = 5_000 * self.LAMPORT
        self.stats = pd.DataFrame()

    def calculate_epoch_stats(self, epoch, epoch_df, df_stake, df_leaders):
        self.stats = self.stats.append({
            'node': range(0, self.nodes),
            'leader_reward': (epoch_df['txCounterVote'] + epoch_df['txCounterNonVote']) * self.LAMPORT / 2 / self.SOL,
        }, ignore_index=True)


leader_reward = []
labels = []

java.SimulatorBuild()
for ddos_nodes in [0, 10, 15, 20]:
    scenario02 = Scenario03()
    p = java.SimulatorSubprocess(nodes=1500, epoch=10, tps=3000, ddos=ddos_nodes).run()
    out.OutputMetadata().parse(scenario02.calculate_epoch_stats)
    leader_reward.append(scenario02.stats['leader_reward'].mean())
    scenario02.stats['node'] = range(0, len(scenario02.stats.index))
    labels.append(ddos_nodes)

df = pd.DataFrame({
    'TPS': leader_reward,
    'Stake': ddos_stake,
}, index=labels)
df.to_csv('scenario03.csv')
ax = df.plot(kind='bar', legend=False)
ax.set_xlabel('#nodes under DDoS attack', fontsize=10)
ax.set_ylabel('TPS', fontsize=10)
plt.show()