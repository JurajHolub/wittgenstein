import pandas as pd
from wsparser.SolanaParser import SolanaParser as out
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick

class Scenario02:
    """
    Simulate X epochs with 1500 nodes (same as real network). The nodes stake is taken from the real network.
    Set up TPS to the real one (around 3k).
    Re-simulate with ddos-attack. Intruders attacks N richest nodes (10, 15, 20).
    Check the TPS drop according to the size of ddos attack.
    """

    def __init__(self):
        self.stake = pd.DataFrame()
        self.tps = pd.DataFrame()

    def calculate_epoch_stats(self, epoch, epoch_df, df_stake, df_leader):
        self.stake= self.stake.append(df_stake, ignore_index=True)
        self.tps = self.tps.append({
            'epoch_end_time': epoch_df['arriveTime'].mean(),
            'tps_total': (epoch_df['txCounterVote'] + epoch_df['txCounterNonVote']).sum(),
        }, ignore_index=True)


mean_tps = []
ddos_stake = []
labels = []

java.SimulatorBuild()
for ddos_nodes in range(0, 50, 5):
    scenario02 = Scenario02()
    p = java.SimulatorSubprocess(protocol='solana', nodes=1500, epoch=5, tps=500, ddos=ddos_nodes).run()
    out.Parser().parse(scenario02.calculate_epoch_stats)

    scenario02.stake = scenario02.stake.groupby('node').mean().reset_index()
    scenario02.stake.sort_values(by=['stake'], ascending=False)
    under_ddos = scenario02.stake.loc[scenario02.stake['node'].isin(range(0, ddos_nodes))]
    ddos_stake.append(under_ddos['stake'].sum() / scenario02.stake['stake'].sum())
    mean_tps.append(scenario02.tps['tps_total'].sum() / scenario02.tps['epoch_end_time'].max() * 1000)
    labels.append(ddos_nodes)

fig = plt.figure()
gs = gridspec.GridSpec(2, 1, height_ratios=[1, 1])
ax0 = fig.add_subplot(gs[0])
ax0.tick_params(labelbottom=False)
ax1 = fig.add_subplot(gs[1])
ax0.set_ylabel("TPS")
df = pd.DataFrame({
    'TPS': mean_tps
}, index=labels).plot.bar(rot=45, ax=ax0, legend=False, color='r')
pd.DataFrame({
    'Nedostupné uzly': [stake*100 for stake in ddos_stake],
    'Dostupné uzly': [(1-stake)*100 for stake in ddos_stake],
}, index=labels).plot.bar(rot=45, ax=ax1, stacked=True)
ax1.set_ylabel("Hlasovací podiel")
ax1.set_xlabel("Počet nedostupných uzlov")
ax1.yaxis.set_major_formatter(mtick.PercentFormatter())
ax0.grid(axis="y", linestyle='--')
ax1.grid(axis="y", linestyle='--')
plt.tight_layout()
plt.show()