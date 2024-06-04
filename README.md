[![Documentation](https://img.shields.io/badge/Documentation-Read-purple)](https://variantsync.github.io/trace-boosting-eval/)
[![License](https://img.shields.io/badge/License-GNU%20LGPLv3-blue)](LICENSE.LGPL3)

# TODOS 
- batch scripts
- Implement setup validation!
- replication argument 
- rename execution script
- rename properties file
- automated download of data 
- unpacking of data 
- add hints about data

# Evaluation Artifact for "Give an Inch and Take a Mile? Effects of Adding Reliable Knowledge to Heuristic Feature Tracing"

This project comprises the empirical evaluation for our [trace boosting]() algorithm implemented in a separate repository as a library. 
 Our algorithm was designed to enhance retroactive feature tracing with proactively collected feature traces. 
It is particularly useful for projects with multiple product variants, where it can improve the accuracy and efficiency of the tracing process. 

This repository comprises artifacts for our paper _Give an Inch and Take a Mile? Effects of Adding Reliable Knowledge to Heuristic Feature Tracing_ which has been accepted at the International Systems and Product Line Conference (SPLC 2024).

## Obtaining the Artifacts
Clone the repository to a location of your choice using [git](https://git-scm.com/):
  ```sh
  git clone https://github.com/VariantSync/trace-boosting-eval.git  
```
## Requirements and Installation

### Setup Instructions
* Install [Docker](https://docs.docker.com/get-docker/) on your system and start the [Docker Daemon](https://docs.docker.com/config/daemon/).
* Open a terminal and navigate to the project's root directory
* Build the docker image by calling the build script corresponding to your OS
  ```sh
  # Windows:
  build-docker-image.bat
  # Linux | MacOS:
  build-docker-image.sh
  ```
* You can validate the installation by calling the validation corresponding to your OS. The validation should take about
  `30 minutes` depending on your system.

  ```shell
  # Windows:
  experiment.bat validate
  # Linux | MacOS:
  experiment.sh validate
  ```
  The script will generate figures and tables similar to the ones presented in our paper. They are automatically saved to
  `./results`.

## Running the Experiments Using Docker

* All of the commands in this section are assumed to be executed in a terminal with working directory at RaQuN's project
root.
* You can stop the execution of any experiment by running the following command in another terminal:
  ```shell
  # Windows Command Prompt:
  stop-execution.bat
  # Windows PowerShell:
  .\stop-execution.bat
  # Linux | MacOS
  ./stop-execution.sh
  ```
Stopping the execution may take a moment.

### Running all Experiments
You can repeat the experiments exactly as presented in our paper. The following command will execute 30 runs of the experiments
for RQ1 and RQ2.
```shell
# Windows Command Prompt:
execute.bat replication
# Windows PowerShell:
.\execute.bat replication
# Linux | MacOS
./execute.sh replication
```
```
Expected Average Runtime for all experiments (@2.90GHz): 2460 hours or 102 days.

We provide instructions on how to parallelize the experiments for a shorter total runtime in the next sections.
```

### Running Specific Experiments
Due to the considerable runtime of running all experiments in a single container, we offer possibilities to run 
individual experiments and repetitions of specific experiments in parallel.
You can run a single experiment repetition for any of the RQs (e.g., `experiment.bat RQ1` executes RQ1). If you want to 
run multiple containers in parallel, you simply have to open a new terminal and start the experiment there as well. 
```shell
# Windows Command Prompt:
experiment.bat (RQ1|RQ2|RQ3)  
# Windows PowerShell:
.\experiment.bat (RQ1|RQ2|RQ3)
# Linux | MacOS
./experiment.sh (RQ1|RQ2|RQ3)
```
#### Runtime
`Expected Average Runtime for one Repetition of RQ1 (@2.90GHz): 4 hours` (Repeated 30 times for the paper)

`Expected Average Runtime for one Repetition of RQ2 (@2.90GHz): 8 hours` (Repeated 30 times for the paper)

`Expected Average Runtime for one Repetition of RQ3 (@2.90GHz): 2100 hours or 87 days` (Repeated 1 time for the paper)

#### Running RQ3 on Specific ArgoUML Subsets
Due to the large runtime of RQ3, we made it possible to run the experiments on individual subsets in parallel. There
are 30 subsets for each subset size. You can filter these subsets for the experiment by providing a `SUBSET_ID`. `SUBSET_ID`
has to be a natural number in the interval `[1, 30]` (e.g., `experiment.bat RQ3 1` will run RQ for all subsets with ID 1).
Hereby, you can start multiple Docker containers in 
parallel.
```shell
# Windows Command Prompt:
experiment.bat RQ3 SUBSET_ID  
# Windows PowerShell:
.\experiment.bat RQ3 SUBSET_ID  
# Linux | MacOS
./experiment.sh RQ3 SUBSET_ID
```

#### Runtime
`Expected Average Runtime for one Repetition of RQ3 With a Specific SUBSET_ID (@2.90GHz): 70 hours` 
(Repeated 1 time for each of the 30 valid `SUBSET_ID`)

```
We ran the experiments in parallel on a compute server with 240 CPU cores (2.90GHz) and 1TB RAM. 
For RQ1 and RQ2, we set the number of repetitions to 2, for RQ3 to 1. Then, we executed the following sequential steps:
  - 15 parallel executions of RQ1 by calling 'experiment.(sh|bat) RQ1' in 15 different terminal sessions.
  - 15 parallel executions of RQ2 by calling 'experiment.(sh|bat) RQ2' in 15 different terminal sessions.
  - 30 parallel executions of RQ3 by calling 'experiment.(sh|bat) RQ3 SUBSET_ID' with the 30 different SUBSET_IDs in 
    different terminal sessions
The total runtime was about 3-4 days.
```

### Result Evaluation
You can run the result evaluation by calling the experiment script with `evaluate`. The
script will consider all data found under `./results`.
```shell
# Windows Command Prompt:
experiment.bat evaluate
# Windows PowerShell:
.\experiment.bat evaluate  
# Linux | MacOS
./experiment.sh evaluate
```
`Expected Average Runtime for all experiments (@2.90GHz): a few seconds`
The script will generate figures and tables similar to the ones presented in our paper. They are automatically saved to
`./results/eval-results`.

### Docker Experiment Configuration
By default, the properties used by Docker are configured to run the experiments as presented in our paper. We offer the 
possibility to change the default configuration. 
* Open the properties file in [data](data) which you want to adjust
  * [`replication.properties`](data/replication.properties) configures the experiment execution 
  of `execute.(bat|sh) replication`
  * [`validation.properties`](data/validation.properties) configures the experiment execution of  
 `execute.(bat|sh) validation`
* Change the properties to your liking
* Rebuild the docker image as described above
* Delete old results in the `./results` folder
* Start the experiment as described above

### Clean-Up
The more experiments you run, the more space will be required by Docker. The easiest way to clean up all Docker images and
containers afterwards is to run the following command in your terminal. Note that this will remove all other containers and images
 as well:
```
docker system prune -a
```
Please refer to the official documentation on how to remove specific [images](https://docs.docker.com/engine/reference/commandline/image_rm/) and [containers](https://docs.docker.com/engine/reference/commandline/container_rm/) from your system.

