#! /bin/bash
replication() {
	echo "Running replication"
	ls -l
	java -Xmx128g -Dtinylog.configuration=/home/user/tinylog.properties -jar traceboosting-eval-jar-with-dependencies.jar replication.properties
}

validation() {
	echo "Running validation"
	ls -l
	java -Xmx128g -Dtinylog.configuration=/home/user/tinylog.properties -jar traceboosting-eval-jar-with-dependencies.jar validation.properties
}

if [ ! -d "data/ground-truth" ]; then
	./setup.sh
fi

if [ "$1" == "replication" ]; then
	replication
elif [ "$1" == "validation" ]; then
	validation
else
	echo "Invalid argument. Please use 'replication' or 'validation'."
fi
