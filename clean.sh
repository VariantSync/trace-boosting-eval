#! /bin/bash
echo "Cleaning all related Docker data. This may take a moment..."
echo "Trying to stop running containers..."
docker stop "$(docker ps -a -q --filter "ancestor=boosting")"
echo "Removing boosting image..."
docker image rm boosting
echo "Removing boosting containers..."
docker container rm "$(docker ps -a -q --filter "ancestor=boosting")"
echo "...done."
