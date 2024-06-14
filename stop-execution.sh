#!/usr/bin/env bash

echo "Stopping Docker container. This will take a moment..."
docker stop "$(docker ps -a -q --filter "ancestor=boosting")"
echo "...done."
