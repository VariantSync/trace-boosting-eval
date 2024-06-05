@echo off
docker run --rm -v "%cd%\results":"/home/user/results" boosting %*

