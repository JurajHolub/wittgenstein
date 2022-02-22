import subprocess

path_to_simulator = '../../wittgenstein-simulator'


class SimulatorBuild:
    def __init__(self):
        subprocess.Popen(['gradle', 'clean', 'shadowJar'], cwd=path_to_simulator, shell=True).communicate()


class SimulatorSubprocess:

    def __init__(self, nodes, epoch, tps, ddos=0):
        self.simulation = subprocess.Popen(
            [
                'java',
                '-Xms6000m', '-Xmx12048m',
                '-classpath', 'protocols/build/libs/wittgenstein-all.jar',
                'net.consensys.wittgenstein.protocols.solana.Solana',
                '-n', f'{nodes}',
                '-e', f'{epoch}',
                '--tps', f'{tps}',
                '--ddos-attack', f'{ddos}',
            ],
            cwd=path_to_simulator,
            shell=True,
        )

    def run(self):
        return self.simulation.communicate()
