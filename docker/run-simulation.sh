#! /bin/bash
evaluation() {
	echo "Running evaluation"
	ls -l
	java -Xmx128g -Dtinylog.configuration=/home/user/tinylog.properties -jar traceboosting-eval-jar-with-dependencies.jar
}

evaluation
