#!/usr/bin/env bash

echo "Starting $1"
docker run --rm -v "$(pwd)/results":"/home/user/results" boosting "$@"

echo "Done."
