import json

import pandas as pd
import logger
import os
import re
from wsparser.Parser import Parser


class HarmonyParser(Parser):
    def __init__(self, simulator_path):
        super().__init__(simulator_path)

    def parse_files_metadata(self):
        files_info = []
        for epoch_dir in os.listdir(os.path.join(*[self.simulator_path, 'epochs'])):
            epoch = re.findall('(\\d+)', epoch_dir)[0]
            files_info.append(int(epoch))
            self.epoch_files[epoch] = int(epoch)
        self.epoch_count = int(max(files_info, key=lambda i: i)) + 1

    def read_files(self, epoch_callback):
        for epoch, file in self.epoch_files.items():
            df_blocks = pd.read_csv(os.path.join(*[self.simulator_path, 'epochs', f'epoch{epoch}.csv']))
            with open(os.path.join(*[self.simulator_path, 'stake', f'epoch{epoch}.json']), 'r') as f:
                data = json.loads(f.read())
            df_stake = pd.json_normalize(data)
            df_leaders = pd.read_csv(os.path.join(*[self.simulator_path, 'leaders.csv']))
            epoch_callback(int(epoch), df_blocks, df_stake, df_leaders)

