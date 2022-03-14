import re
import statistics

import pandas as pd
from matplotlib import ticker
import os
import seaborn as sns

import SimulatorSubproces as java
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import matplotlib.ticker as mtick
from wsparser.HarmonyParser import HarmonyParser


class Scenario01:
    """
    Sharding security:
    1000 nodes, 4 shards, different lambdas, uniform stake distribution
    plot heatmap of spearman correlation for each shard
    """

    def __init__(self, simulator_path):
        self.stake = {}
        self.blocks = {}
        self.spearman = pd.DataFrame()
        self.leaders = pd.DataFrame()
        self.simulator_path = simulator_path
        self.output = os.path.abspath(os.path.join(*['output', 'harmony', 'scenario01']))

    def init(self):
        os.makedirs(self.output)
        java.SimulatorBuild(self.simulator_path)

    def calculate_epoch_stats(self, epoch, epoch_df, df_stake, df_leader):
        epoch_df = epoch_df.sort_values(by=['slot', 'shard', 'node'], ascending=True)
        self.blocks[epoch] = epoch_df
        self.stake[epoch] = df_stake
        self.leaders = df_leader

    def simulate(self, process: java.SimulatorSubprocess):
        process.run()

    def scrap(self, process: java.SimulatorSubprocess):
        HarmonyParser(self.simulator_path).parse(self.calculate_epoch_stats)
        # self.mean_msg_transmit(self.blocks, self.leaders, process.nodes, process.token_lambda, process.shards)
        # self.plot_msg_ratio(blocks, stake, process.nodes, process.token_lambda)
        # self.plot_token_in_shards(self.stake[0], process.nodes, process.token_lambda, process.shards)

    def mean_stake(self, stake):
        df = pd.concat(stake.values(), ignore_index=True)
        return df.groupby(by=['node'], as_index=False).mean()

    def save_plot(self, filename):
        file = os.path.abspath(os.path.join(*[self.output, f'{filename}.png']))
        plt.savefig(file)
        file = os.path.abspath(os.path.join(*[self.output, f'{filename}.pdf']))
        plt.savefig(file)

    def mean_msg_transmit(self, nodes, token_lambda, shards):
        leaders_acum = []
        non_leaders_acum = []
        total_acum = []
        for epoch in self.blocks.keys():
            df = self.blocks[epoch].groupby(by=['node'], as_index=False).max()
            epoch_leaders = self.leaders[self.leaders['epoch'] == epoch]
            epoch_leaders = df[df['node'].isin(list(epoch_leaders['node']))]
            epoch_non_leaders = df[~df['node'].isin(list(epoch_leaders['node']))]
            leaders_acum.append(epoch_leaders)
            non_leaders_acum.append(epoch_non_leaders)
            total_acum.append(df)

        epoch_leaders = pd.concat(leaders_acum, ignore_index=True)
        epoch_non_leaders = pd.concat(non_leaders_acum, ignore_index=True)
        total = pd.concat(total_acum, ignore_index=True)
        slots = epoch_leaders['slot'].max()
        leader_received, leader_sent = epoch_leaders['msgReceived'].mean(), epoch_leaders['msgSent'].mean()
        non_leader_received, non_leader_sent = epoch_non_leaders['msgReceived'].mean(), epoch_non_leaders[
            'msgSent'].mean()
        df_msg = pd.DataFrame({
            'tpb': total['transactions'].sum() / slots,
            'bytes_sent': total['bytesReceived'].sum() / slots,
            'bytes_received': total['bytesSent'].sum() / slots,
            'total_sent': total['msgReceived'].sum() / slots,
            'total_received': total['msgSent'].sum() / slots,
            'leader_received': [leader_received / slots],
            'leader_sent': [leader_sent / slots],
            'non_leader_received': [non_leader_received / slots],
            'non_leader_sent': [non_leader_sent / slots],
            'nodes': [nodes],
            'token_lambda': [token_lambda]
        })
        file = os.path.abspath(os.path.join(*[self.output, f'{nodes}nodes-{shards}shards-msg-transmit.csv']))
        if os.path.exists(file):
            df_msg.to_csv(file, mode='a', index=False, header=False)
        else:
            df_msg.to_csv(file, index=False)

    def plot_shard_stake_ratio(self, blocks, stake):
        epoch_nodes = []
        epoch_stakes = []
        epoch_shards = []

        stake_total = stake['stake'].sum()
        for index, row in stake.iterrows():
            epoch_stakes.append(row['stake'] / stake_total * 100)
            epoch_nodes.append(row['node'])
            if (blocks['node'] == row['node']).any():
                node_blocks = blocks[blocks['node'] == row['node']]
                epoch_shards.append(len(list(node_blocks['shard'].unique())))
            else:
                epoch_shards.append(0)
        fig = plt.figure()
        gs = gridspec.GridSpec(2, 1, height_ratios=[1, 1])
        ax0 = fig.add_subplot(gs[0])
        ax0.tick_params(labelbottom=False)
        ax1 = fig.add_subplot(gs[1])
        pd.DataFrame({
            'Podiel': epoch_stakes,
        }, index=epoch_nodes).plot.line(rot=45, ax=ax0)
        pd.DataFrame({
            '# shards': epoch_shards,
        }, index=epoch_nodes).plot.line(rot=45, ax=ax1)
        ax0.set_yscale('log')
        ax0.yaxis.set_major_formatter(ticker.FuncFormatter(lambda y, _: '{:g}%'.format(y)))
        ax0.grid(axis="y", linestyle='--')
        ax1.grid(axis="y", linestyle='--')
        plt.tight_layout()
        plt.show()

    def plot_msg_ratio(self, blocks, stake, nodes, token_lambda):
        df = pd.DataFrame({
            'node': [node for node in stake['node']],
            'sent': [blocks[blocks['node'] == node]['msgSent'].sum() for node in stake['node']],
            '#shards': [blocks[blocks['node'] == node]['shard'].nunique() for node in stake['node']]
        })
        df = df.fillna(0)
        gk = df.groupby('#shards')

        msgs = []
        count = []
        labels = []
        for i in range(blocks['shard'].nunique() + 1):
            if gk.groups.get(i) is None:
                msgs.append(0)
                count.append(0)
            else:
                df = gk.get_group(i)
                msgs.append(df['sent'].mean() / blocks['slot'].max())
                count.append(df['node'].nunique())
            labels.append(i)

        fig = plt.figure()
        gs = gridspec.GridSpec(2, 1, height_ratios=[1, 1])
        ax0 = fig.add_subplot(gs[0])
        # ax0.tick_params(labelbottom=False)
        ax1 = fig.add_subplot(gs[1])
        pd.DataFrame({
            '#shards': count,
        }, index=labels).plot(kind='bar', ax=ax0, legend=False)
        pd.DataFrame({
            '#msg sent': msgs,
        }, index=labels).plot(kind='bar', ax=ax1, legend=False)
        ax0.grid(axis="y", linestyle='--')
        ax0.set_ylabel('Počet uzlov')
        ax1.set_xlabel('Počet shard')
        ax1.set_ylabel('Počet poslaných správ')
        ax1.set_xlabel('Počet shard')
        ax1.grid(axis="y", linestyle='--')
        plt.tight_layout()
        # plt.show()
        self.save_plot(f'msg-network{nodes}-lambda{token_lambda}')

    def mean_shard_nodes_count(self):
        shard_node_count = []
        for epoch in self.stake.keys():
            shard = self.stake[epoch].loc[:, self.stake[epoch].columns.str.startswith('shardTokens')]
            for i in range(len(shard.columns)):
                shard_node_count.append((shard[f'shardTokens.{i}'] != 0).sum())
        return statistics.mean(shard_node_count)

    def plot_token_in_shards(self, nodes, token_lambda, shards):
        fig = plt.figure()
        stake = self.mean_stake(self.stake)
        stake = stake.sort_values(by=['stake'], ascending=True)

        shard_node_count = self.mean_shard_nodes_count()

        df = stake.loc[:, stake.columns.str.startswith('shardTokens')]
        df['Podiel celkom'] = stake['stake'].values
        df['Tokeny celkom'] = stake['tokens'].values
        df.columns = df.columns.str.replace('shardTokens.', 'Shard ', regex=True)
        r = df.hist()
        for ax in [j for i in r for j in i]:
            if ax.axes.title.get_text() == 'Podiel celkom':
                for patch in ax.patches:
                    patch.set_color('r')
            elif ax.axes.title.get_text() == 'Tokeny celkom':
                for patch in ax.patches:
                    patch.set_color('g')
            else:
                ax.tick_params(labelbottom=False)
        plt.suptitle(f'Sieť = {nodes} uzlov, λ = {token_lambda}, priemerný počet uzlov v sharde {shard_node_count}')
        plt.tight_layout()
        self.save_plot(f'{nodes}nodes-{shards}shards-{token_lambda}lambda-stake')

    def plot_msg_transmit(self):
        fig = plt.figure()
        lines = {}
        for file in os.listdir(self.output):
            if not file.endswith('msg-transmit.csv'):
                continue
            df = pd.read_csv(os.path.join(*[self.output, file]))
            nodes, shards = re.findall('(\\d+)', file)
            lines[f'MB/slot'] = list(df['bytes_sent'] * 1e-06)
            lines[f'MB/sec'] = list(df['bytes_sent'] * 1e-06 / 2)
        ax = pd.DataFrame(lines, index=list(df['token_lambda'])).plot.line(marker='o')
        ax.set_ylabel('Odoslané dáta')
        ax.set_xlabel('λ')
        plt.suptitle(f'{nodes} nodes, {shards} shards')
        self.save_plot(f'{nodes}nodes-{shards}shards-transmit-stats')

    def plot_tps(self):
        fig = plt.figure()
        lines = {}
        for file in os.listdir(self.output):
            if not file.endswith('msg-transmit.csv'):
                continue
            df = pd.read_csv(os.path.join(*[self.output, file]))
            nodes, shards = re.findall('(\\d+)', file)
            lines[f'Tx/slot'] = list(df['tpb'])
            lines[f'Tx/sec'] = list(df['tpb'] / 2)
        ax = pd.DataFrame(lines, index=list(df['token_lambda'])).plot.line(marker='o')
        ax.set_xlabel('lambda')
        plt.suptitle(f'{nodes} nodes, {shards} shards')
        self.save_plot(f'{nodes}nodes-{shards}shards-tps')

    def add_shard_correlation(self, shards, token_lambda):
        stake = self.stake[0]
        stake = stake.sort_values(by='stake')
        df_plot = pd.DataFrame({
            'stake': list(stake['stake']),
            'Celkom': list(stake['tokens']),
        })

        for i in range(shards):
            shard = f'shardTokens.{i}'
            shard_nodes = (stake[shard] != 0)
            # x = pd.DataFrame({
            #     f'Shard {i}': stake[shard_nodes][shard].astype(float),
            #     'Total': stake[shard_nodes]['stake'].astype(float),
            # })
            # print(x.corr(method='spearman'))
            df_plot[f'Shard {i}'] = list(stake[f'shardTokens.{i}'])

        coeaf = df_plot.corr(method='spearman')[['stake']].iloc[1:, :]
        self.spearman[f'λ={token_lambda}\n μ={self.mean_shard_nodes_count()}'] = coeaf

    def plt_shard_correlation(self):
        fig = plt.figure()
        sns.heatmap(self.spearman, vmin=-1, vmax=1)
        plt.xticks(rotation=0)
        fig.tight_layout()
        self.save_plot(f'shard-correlation')

    def shard_control_attack(self, shards):
        attacks = {}
        for epoch in self.stake.keys():
            stake = self.stake[epoch]
            total_stake = stake['stake'].sum()
            total_byzantine = stake[stake['byzantine']]['stake'].sum()
            byzantine_nodes = stake[stake['byzantine']]['stake'].count()

            shard = stake.loc[:, stake.columns.str.startswith('shardTokens')]
            for i in range(len(shard.columns)):
                shard_nodes = (shard[f'shardTokens.{i}'] != 0)
                shard_stake = stake[shard_nodes]['tokens'].sum()
                shard_byzantine = stake[shard_nodes][stake[shard_nodes]['byzantine']]['tokens'].sum()
                attacks.setdefault(f'Hlasovací podiel', []).append(shard_byzantine / shard_stake * 100)
                attacks.setdefault(f'Shard', []).append(i)
                attacks.setdefault(f'Epocha', []).append(epoch)
        df = pd.DataFrame(attacks)

        g = sns.FacetGrid(df, col="Shard", col_wrap=2, aspect=2)
        g.map(sns.regplot, 'Epocha', 'Hlasovací podiel', fit_reg=False, scatter_kws={'s': 20})
        for ax in g.axes.flatten():
            ax.axhline(y=(total_byzantine / total_stake * 100), color='r', linewidth=2)
            ax.yaxis.set_major_formatter(mtick.PercentFormatter())
        self.save_plot(f'byzantine{byzantine_nodes}-shard-distribution')

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
        self.save_plot(f'byzantine{byzantine_nodes}-shard-kde')
