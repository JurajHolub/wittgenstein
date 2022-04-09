import pandas as pd
import json

# 14.3.2022
# https://www.blockchain.com/charts/avg-block-size
with open('avg-block-size.json', 'r') as f:
    data = json.loads(f.read())
block_size = pd.json_normalize(data['values'])

# 14.3.2022
# https://www.blockchain.com/charts/n-transactions-per-block
with open('n-transactions-per-block.json', 'r') as f:
    data = json.loads(f.read())
transactions = pd.json_normalize(data['values'])
transaction_pre_block = transactions['y']

header_size = 80
block_tx_bytes = block_size['y'] * 1e06 - header_size

avg_tx_size = (block_tx_bytes / transaction_pre_block).mean()
print('Average Bitcoin transaction size in 2021: ', avg_tx_size)
