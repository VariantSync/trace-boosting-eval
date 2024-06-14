#!/usr/bin/env bash

USER_ID=$(id -u ${SUDO_USER:-$(whoami)})
GROUP_ID=$(id -g ${SUDO_USER:-$(whoami)})
PLATFORM=linux/arm64/v8

docker build --network=host --platform $PLATFORM --build-arg USER_ID=$USER_ID --build-arg GROUP_ID=$GROUP_ID -t boosting .
