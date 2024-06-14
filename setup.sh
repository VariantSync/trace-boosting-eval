#!/usr/bin/env bash

# Download the ground-truth.zip file
curl -o data/ground-truth.zip https://zenodo.org/records/11472597/files/ground-truth.zip?download=1

# Download the repos.zip file
curl -o data/repos.zip https://zenodo.org/records/11472597/files/repos.zip?download=1

# Unzip the ground-truth.zip file
unzip data/ground-truth.zip -d data

# Unzip the repos.zip file
unzip data/repos.zip -d data

# Clean zips
rm data/ground-truth.zip
rm data/repos.zip
