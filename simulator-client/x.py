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

res = requests.post('https://adapools.org/ext/dataset.php?04042211&_=1649070175199')

data = json.loads(res.text)
data = [item[10] for item in data['data']]
nodes = [i for i in range(len(data))]
df = pd.DataFrame({
    'node': nodes,
    'stake': data
})
df.set_index('node', inplace=True)

df.to_csv('CardanoStake--2022-04-04.csv')
print(len(data))