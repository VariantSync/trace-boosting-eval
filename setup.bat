@echo off

# Download the ground-truth.zip file
curl -o data/ground-truth.zip https://zenodo.org/records/11472597/files/ground-truth.zip?download=1

# Download the repos.zip file
curl -o data/repos.zip https://zenodo.org/records/11472597/files/repos.zip?download=1

# Unzip the ground-truth.zip file
Expand-Archive data/ground-truth.zip -DestinationPath data

# Unzip the repos.zip file
Expand-Archive data/repos.zip -DestinationPath data

# Clean zips
rm data/ground-truth.zip
rm data/repos.zip
