@echo off
docker build -t --build-arg USER_ID=1000 --build-arg GROUP_ID=1000 boosting .
