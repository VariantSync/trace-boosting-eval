#! /bin/bash
replication() {
	echo "Running replication"
	java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Dtinylog.configuration=/home/user/tinylog.properties -jar traceboosting-eval-jar-with-dependencies.jar replication.properties
}
validation() {
    echo "Running validation"
    java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Dtinylog.configuration=/home/user/tinylog.properties -jar traceboosting-eval-jar-with-dependencies.jar validation.properties
}

plotting() {
	echo "Running plot generation"
	cd python
	echo "set venv"
	rm -rf venv
	virtualenv venv
	source venv/bin/activate
	pip install -r requirements.txt
	python generatePlots.py
	cd ..
}

if [ ! -d "data/ground-truth" ]; then
	./setup.sh
fi

if [ "$1" == "replication" ]; then
	replication
elif [ "$1" == "validation" ]; then
	validation
elif [ "$1" == "plotting" ]; then
	plotting
else
	echo "Invalid argument. Please use 'replication', 'validation', or 'plotting'."
fi
