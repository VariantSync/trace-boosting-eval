# Trace boosting Eval

## Docker setup 
- install docker 
- start the docker daemon according to your distro
- build the docker image by calling the build script
```shell
./build.sh
```
- start the experiments
```shell 
./execute.sh evaluation
``` 

> Hint: You can stop the evaluation any time by calling the stop script `./stop-execution.sh`

## Docker eval setup 
It is possible to configure the evaluation run by Docker by changing the properties files for the different RQs under `data/` and then rebuilding and running the docker image.



# TODO
- Unzip ground-truth
- Unzip repos
