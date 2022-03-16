from scenarios.harmony.scenario01 import Scenario01
from scenarios.harmony.scenario02 import Scenario02
from scenarios.harmony.scenario03 import Scenario03
from scenarios.harmony.scenario04 import Scenario04
import argparse

output_path = 'output'

parser = argparse.ArgumentParser()
parser.add_argument('--harmony', action='store_true', help='Simulation of Harmony.')
parser.add_argument('--scenario01', action='store_true', help='Sharding PoS stake disribution.')
parser.add_argument('--scenario02', action='store_true', help='Byzantine shard attack.')
parser.add_argument('--scenario03', action='store_true', help='Harmony throughput.')
parser.add_argument('--scenario04', action='store_true', help='Leader DDoS attack.')
args = parser.parse_args()

if not args.harmony:
    parser.exit()

scenario = None
if args.scenario01:
    scenario = Scenario01(output_path)
if args.scenario02:
    scenario = Scenario02(output_path)
if args.scenario03:
    scenario = Scenario03(output_path)
if args.scenario04:
    scenario = Scenario04(output_path)
if scenario is None:
    parser.exit()

scenario.simulate()
scenario.analyze()
