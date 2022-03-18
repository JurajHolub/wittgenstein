# Harmony scenarios
*Wittgenstain simulator server must already running!*

## Prerequisities
`$ pip install -r requirements.txt`

## How to use
Run harmony scenarios:
* `client.py --harmony --scenario01` - Sharding distribution security:
  * 1000 nodes, 4 shards, various lambdas, uniform stake distribution 
  * Plot heatmap of spearman correlation for each shard
* `client.py --harmony --scenario02` - Shard control attack:
  * 1000 nodes, 4 shards, 600 lambda, uniform stake distribution, various byzantine nodes
  * Plot histogram of voting stake for byzantine nodes (1000 epochs)
* `client.py --harmony --scenario03` - Sharding throughput:
  * transaction with size >600B
  * plot Tx/slot, MB/slot for various network size
* `client.py --harmony --scenario04` - DDoS leaders
  * Simulate DDoS attack to a leaders.

View results in the directory `/output`

# Solana scenarios
*Not supported yet*