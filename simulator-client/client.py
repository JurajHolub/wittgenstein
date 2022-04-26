from scenarios.harmony.scenario01 import Scenario01 as HarmonyScenario01
from scenarios.harmony.scenario02 import Scenario02 as HarmonyScenario02
from scenarios.harmony.scenario03 import Scenario03 as HarmonyScenario03
from scenarios.harmony.scenario04 import Scenario04 as HarmonyScenario04
from scenarios.harmony.scenario05 import Scenario05 as HarmonyScenario05
from scenarios.solana.scenario01 import Scenario01 as SolanaScenario01
from scenarios.solana.scenario02 import Scenario02 as SolanaScenario02
from scenarios.solana.scenario03 import Scenario03 as SolanaScenario03
from scenarios.solana.scenario04 import Scenario04 as SolanaScenario04
from scenarios.ouroboros.scenario01 import Scenario01 as OuroborosScenario01
from scenarios.ouroboros.scenario02 import Scenario02 as OuroborosScenario02
from scenarios.ouroboros.scenario03 import Scenario03 as OuroborosScenario03
from scenarios.comparison.scenario01 import Scenario01 as ComparisonScenario01
from scenarios.comparison.scenario02 import Scenario02 as ComparisonScenario02
from argparse import ArgumentParser

output_path = 'output'

parser = ArgumentParser('client.py')
parser.add_argument('--mongoserver', type=str, required=True, help='Address to mongo server. In case of '
                                                                   'release docker it is \'mongodb\'. In '
                                                                   'case of debug \'localhost:27017\'.')
subparsers = parser.add_subparsers(dest='command')

harmony = subparsers.add_parser('harmony', help='Simulation of Harmony.')
harmony.add_argument('--scenario01', action='store_true', help='Sharding PoS stake disribution.')
harmony.add_argument('--scenario02', action='store_true', help='Byzantine shard attack.')
harmony.add_argument('--scenario03', action='store_true', help='Harmony throughput.')
harmony.add_argument('--scenario04', action='store_true', help='Leader DDoS attack.')
harmony.add_argument('--scenario05', action='store_true', help='Leader DDoS attack with VRF feature.')

solana = subparsers.add_parser('solana', help='Simulation of Solana.')
solana.add_argument('--scenario01', action='store_true', help='Ratio of voting / nonvoting transactions.')
solana.add_argument('--scenario02', action='store_true', help='Leader DDoS attack.')
solana.add_argument('--scenario03', action='store_true', help='Throughput in bytes.')
solana.add_argument('--scenario04', action='store_true', help='Leader DDoS attack with VRF feature.')

ouroboros = subparsers.add_parser('ouroboros', help='Simulation of Ouroboros.')
ouroboros.add_argument('--scenario01', action='store_true', help='DoS attack to leader schedule.')
ouroboros.add_argument('--scenario02', action='store_true', help='Fork and finality.')
ouroboros.add_argument('--scenario03', action='store_true', help='DoS attack to VRF.')

comparison = subparsers.add_parser('comparison', help='Comparison of Solana, Harmony and Ouroboros.')
comparison.add_argument('--scenario01', action='store_true', help='Data throughput comparison for Solana and Harmony.')
comparison.add_argument('--scenario02', action='store_true', help='VRF ddos attack effectivity comparison.')

args = parser.parse_args()

scenario = None

if args.command == 'harmony':
    if args.scenario01:
        scenario = HarmonyScenario01(output_path)
    if args.scenario02:
        scenario = HarmonyScenario02(output_path)
    if args.scenario03:
        scenario = HarmonyScenario03(output_path)
    if args.scenario04:
        scenario = HarmonyScenario04(output_path)
    if args.scenario05:
        scenario = HarmonyScenario05(output_path)
elif args.command == 'solana':
    if args.scenario01:
        scenario = SolanaScenario01(output_path)
    if args.scenario02:
        scenario = SolanaScenario02(output_path)
    if args.scenario03:
        scenario = SolanaScenario03(output_path)
    if args.scenario04:
        scenario = SolanaScenario04(output_path)
elif args.command == 'ouroboros':
    if args.scenario01:
        scenario = OuroborosScenario01(output_path)
    if args.scenario02:
        scenario = OuroborosScenario02(output_path)
    if args.scenario03:
        scenario = OuroborosScenario03(output_path)
elif args.command == 'comparison':
    if args.scenario01:
        scenario = ComparisonScenario01(output_path)
    if args.scenario02:
        scenario = ComparisonScenario02(output_path)
else:
    parser.exit()

if scenario is None:
    parser.exit()

if args.mongoserver:
    scenario.mongoserver = args.mongoserver
else:
    parser.exit()

scenario.simulate()
scenario.analyze()
