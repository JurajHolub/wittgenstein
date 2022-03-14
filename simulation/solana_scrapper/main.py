from scenarios.harmony.s1 import Scenario01
from scenarios.harmony.s2 import Scenario02
from scenarios.harmony.s3 import Scenario03

output_path = 'output'

scenario = Scenario03(output_path)
scenario.init()
scenario.simulate()
scenario.analyze()
