import os
import shutil
import json
import pandas as pd
from pymongo import MongoClient

from matplotlib import pyplot as plt


class Scenario:

    def __init__(self, output_path):
        self.stake = {}
        self.leaders = {}
        self.blocks = {}
        self.output_path = output_path
        self.harmony_endpoint = 'http://localhost:8080/w/harmony'

    def init(self):
        if os.path.exists(self.output_path):
            shutil.rmtree(self.output_path)
        os.makedirs(self.output_path)

    def simulate(self):
        pass

    def analyze(self):
        pass

    def save_plot(self, filename):
        file = os.path.abspath(os.path.join(*[self.output_path, f'{filename}.png']))
        plt.savefig(file)
        file = os.path.abspath(os.path.join(*[self.output_path, f'{filename}.pdf']))
        plt.savefig(file)

    def open_epoch_df(self, path):
        with open(path, 'r') as f:
            data = json.loads(f.read())
        return pd.json_normalize(data)

    def get_data_from_mongo(self):
        client = MongoClient()
        return client.simulator
        df = pd.DataFrame.from_dict(db.Epochs)
        pass
        # db.Epochs
        # db.Leaders
        # db.StakeStats