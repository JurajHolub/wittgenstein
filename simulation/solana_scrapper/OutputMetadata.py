import os
import re
import pandas as pd
import logger


class OutputMetadata:
    def __init__(self):
        self.path = '../../wittgenstein-simulator/output'
        self.stats = dict()
        self.epoch_files = {}
        self.epoch_count = 0
        self.node_count = 0

    def parse_file_metadata(self):
        files_info = []
        for epoch_dir in os.listdir(self.path+'/epochs'):
            epoch = re.findall('(\\d+)', epoch_dir)[0]
            for filename in os.listdir(self.path + '/epochs/' + epoch_dir):
                node = re.findall('(\\d+)', filename)[0]
                tmp = (int(epoch), int(node), filename)
                files_info.append(tmp)
        self.epoch_count = int(max(files_info, key=lambda i: i[0])[0]) + 1
        for epoch in range(self.epoch_count):
            self.epoch_files[epoch] = list(filter(lambda file: file[0] == epoch, files_info))
            self.node_count = len(self.epoch_files[epoch])

    def parse(self, epoch_callback):
        self.parse_file_metadata()

        prev_epoch_end = 0
        for epoch, files in self.epoch_files.items():
            df = pd.DataFrame()
            tmp = [df]
            parsed_nodes = 0
            for _, node, filename in files:
                if parsed_nodes % 100 == 0:
                    logger.logging.info(
                        f'Epoch {epoch}/{self.epoch_count}: Parse nodes {"{:.2f}".format(parsed_nodes / self.node_count * 100)}%.')
                df_node = pd.read_csv(f'{self.path}/epochs/epoch{epoch}/{filename}')
                tmp.append(df_node)
                parsed_nodes += 1
            df = pd.concat(tmp, ignore_index=True)
            df = df.groupby(['slot']).mean()
            df_stake = pd.read_csv(f'{self.path}/stake/epoch{epoch}.csv')
            df_leader = pd.read_csv(f'{self.path}/leaders/epoch{epoch}.csv')
            epoch_callback(epoch, df, df_stake, df_leader)
            prev_epoch_end = df['arriveTime'].iloc[-1]
