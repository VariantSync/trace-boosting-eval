[![Documentation](https://img.shields.io/badge/Documentation-Read-purple)](https://variantsync.github.io/trace-boosting-eval/)
[![License](https://img.shields.io/badge/License-GNU%20LGPLv3-blue)](LICENSE.LGPL3)

# TODOS 
- batch scripts
- Implement setup validation!
- replication argument 
- rename execution script
- automated download and unpacking of data in Docker container

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

### (Optional) Download the experiment data
> This is done automatically by the Docker script and is only required if you plan on interacting directly with the data. 

Open a terminal in the cloned directory and execute the setup script to download the required data files containing subject repositories and their ground truth from [Zenodo](https://doi.org/10.5281/zenodo.11472597). 
```sh 
./setup.sh
```

## Requirements and Installation

### Setup Instructions
* Install [Docker](https://docs.docker.com/get-docker/) on your system and start the [Docker Daemon](https://docs.docker.com/config/daemon/).
* Open a terminal and navigate to the project's root directory
* Build the docker image by calling the build script corresponding to your OS
  ```sh
  # Windows:
  build.bat
  # Linux | MacOS:
  build.sh
  ```
* You can validate the installation by calling the validation corresponding to your OS. The validation should take about
  `30 minutes` depending on your system.

  ```shell
  # Windows:
  execute.bat validation
  # Linux | MacOS:
  execute.sh validation
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

