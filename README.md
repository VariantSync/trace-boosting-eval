[![Documentation](https://img.shields.io/badge/Documentation-Read-purple)](https://variantsync.github.io/trace-boosting-eval/)
[![License](https://img.shields.io/badge/License-GNU%20LGPLv3-blue)](LICENSE.LGPL3)

# TODOS 
- Revise README 
- Add hint in README about memory requirements and runtime for the replication 
- Test setup 
  - Windows 
    - replication 
    - validation
  - MacOS
    - replication 
    - validation

# Evaluation Artifact for "Give an Inch and Take a Mile? Effects of Adding Reliable Knowledge to Heuristic Feature Tracing"

This repository comprises the artifact for our paper _Give an Inch and Take a Mile? Effects of Adding Reliable Knowledge to Heuristic Feature Tracing_ 
which is accepted at the 28th ACM International Systems and Software Product Line Conference (SPLC 2024).
The project comprises the source code running the empirical evaluation of our **boosted comparison-based feature tracing** algorithm, 
which is implemented as a [library in a separate repository](https://github.com/VariantSync/trace-boosting). 

Our algorithm is designed to enhance the accuracy of retroactive feature tracing with proactively collected feature traces. 
It is particularly useful for projects with multiple product variants, where it can improve the accuracy and efficiency of the tracing process. 


## Obtaining this Artifact
Clone the repository to a location of your choice using [git](https://git-scm.com/docs/git-clone):
  ```sh
  git clone https://github.com/VariantSync/trace-boosting-eval.git  
```

### (Optional) Download the experiment data
> This is done automatically by the Docker script and is only required if you plan on interacting directly with the data. 

Open a terminal in the cloned directory and execute the setup script to download the required data files containing subject repositories and their ground truth from [Zenodo](https://doi.org/10.5281/zenodo.11472597). 
```sh 
./setup.sh
```

## Getting Started: Requirements and Installation

### Setup Instructions
* Install [Docker](https://docs.docker.com/get-docker/) on your system and start the [Docker Daemon](https://docs.docker.com/config/daemon/).
> Depending on your Docker installation, you might require elevated permission (i.e., sudo) to call the Docker daemon under Linux. 
* Open a terminal and navigate to the project's root directory
* Build the docker image by calling the build script corresponding to your OS
  ```sh
  # Windows:
  build.bat
  # Linux | MacOS:
  build.sh
  ```
* You can validate the installation by calling the validation corresponding to your OS. The validation runs about
  __`30 minutes`__ depending on your system.

  ```shell
  # Windows:
  execute.bat validation
  # Linux | MacOS:
  execute.sh validation
  ```
  The script will generate figures and tables similar to the ones presented in our paper. They are automatically saved to
  `./results`.

## Running the Experiments Using Docker

* All commands in this section are supposed to be executed in a terminal with working directory at the evaluation-repository's project
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
You can repeat the experiments exactly as presented in our paper. 
The following command executes 30 runs of the experiments
for RQ1 and RQ2.

**Please note:** Due to comparing potentially large files, mapped to tree-structures, and feature expressions, 
the entire experiment requires high amounts of RAM (> 24GB) and several hours. 

```shell
# Windows Command Prompt:
execute.bat replication
# Windows PowerShell:
.\execute.bat replication
# Linux | MacOS
./execute.sh replication
```

### Plot the results using docker
Finally, you can plot the results using Docker. 
> If you have not executed the replication or validation, there are no results to analyze and plot, yet. 
> However, we also provide our reported results for which the plots can be generated. 
> To generate the plots shown in our paper, you have to copy the result files (.json) under [reported-results](reported-results) to the [results](results) directory. 
```shell
# Windows Command Prompt:
execute.bat plotting
# Windows PowerShell:
.\execute.bat plotting
# Linux | MacOS
./execute.sh plotting
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

## Generating the plots 
### Make sure there are results to analyse 
If you have not executed the replication or validation, there are no results to analyze and plot, yet. 
However, we also provide our reported results for which the plots can be generated. 
To generate the plots shown in our paper, you have to copy the result files (.json) under [reported-results](reported-results) to the [results](results) directory. 

```bash 
cp reported-results/* results/
```

### Execute the python script

0. **Navigate to the python sources**
```bash
cd python 
```

1. **Install virtualenv** (if you haven't already):
   ```bash
   pip install virtualenv
   ```
2. **Create a virtual environment**:
   Navigate to your project directory and run:
   ```bash
   virtualenv venv
   ```
   This will create a virtual environment named `venv` in your project directory.
3. **Activate the virtual environment**:
   - On Windows, run:
     ```cmd
     .\venv\Scripts\activate
     ```
   - On macOS and Linux, run:
     ```bash
     source venv/bin/activate
     ```
4. **Install the dependencies**:
   With the virtual environment activated, install the dependencies from `requirements.txt` by running:
   ```bash
   pip install -r requirements.txt
   ```
5. **Run the script**:
   With the dependencies installed, you can now run your Python script. For example, if your script is named `script.py`, run:
   ```bash
   python generatePlots.py
   ```
6. **Deactivate the virtual environment** (when you're done):
   To exit the virtual environment, simply run:
   ```bash
   deactivate
