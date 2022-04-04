import os

import pandas as pd
import json
import pandas as pd
import matplotlib.pyplot as plt
import requests
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
from pymongo import MongoClient
import seaborn as sns
from matplotlib.lines import Line2D


#plt.rcParams['text.usetex'] = True

df = pd.read_csv('CardanoStake--2022-04-04.csv')
total = df['stake'].sum()
df['stake'] = (df['stake'] / total) * 100
df.set_index('node', inplace=True)
# ax = df.plot.hist()
# ax.set_yscale('log')
# plt.show()

#results, bin_edges = pd.qcut(df['stake'], q=10, retbins=True)

df['category'] = pd.cut(df.stake,
       bins=[-1., 0.01, 0.05, 0.1, 0.25, 100],
       labels=[
              'Veľmi\nmalý',
              'Malý',
              'Stredný',
              'Veľký',
              'Veľmi\nveľký']
)

fig = plt.figure()
gs = gridspec.GridSpec(2, 1, height_ratios=[1, 1])
ax0 = fig.add_subplot(gs[0])
ax0.tick_params(labelbottom=False)
ax1 = fig.add_subplot(gs[1])
ax0.set_ylabel("Počet")
ax = df['category'].value_counts().plot.bar(rot=45, ax=ax0, color=sns.color_palette("colorblind"))

#ax = sns.countplot(x='category', ax=ax0, data=df)
ax.bar_label(ax.containers[0])
df.groupby(['category']).sum()['stake'].plot.bar(rot=0, ax=ax1, color=sns.color_palette("colorblind"))
ax1.set_ylabel("Spoločný podiel [%]")
ax0.set_xlabel('')
ax1.set_xlabel('Podiel')
#ax1.yaxis.set_major_formatter(mtick.PercentFormatter())
ax0.grid(axis="y", linestyle='--')
ax1.grid(axis="y", linestyle='--')
plt.tight_layout()
#plt.show()
file = os.path.abspath(os.path.join(*[f'CardanoStake-2022-04-04.pdf']))
plt.savefig(file)