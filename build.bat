@echo off
docker build --build-arg USER_ID=1000 --build-arg GROUP_ID=1000 -t boosting .
