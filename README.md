# How to use

## Prerequisities
* [docker-compose](https://docs.docker.com/get-docker)
* Python3 with the following packages
    * requests
    * pandas
    * matplotlib
    * seaborn
    * pymongo

## Architecture

Simulator is client-server application:
* `simulator-server` - Simulator Wittgenstein.
* `simulator-client` - Python script that executes various scenarios of simulation and analyze results.

## Build
Simulator (`simulator-server`) is containerized with [docker-compose](https://docs.docker.com/get-docker):
* `simulator`: Java Spring server that runs Wittgenstein simulation.
* `mongodb`: Database for continuous simulation recording.

Set up simulator server (it may take a few minutes):
1. `docker-compose build`.
2. `docker-compose up`

Run specific simulation scenario from `simulator-client`:
1. e.g. `python simulation-client/client.py --harmony --scenario03`
2. View simulation results in `simulation-client/client/output`

