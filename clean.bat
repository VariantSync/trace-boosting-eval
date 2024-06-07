@echo "Cleaning all related Docker data. This may take a moment..."

@echo "Trying to stop running containers..."
@FOR /f "tokens=*" %%i IN ('docker ps -a -q --filter "ancestor=boosting"') DO docker stop %%i

@echo "Removing boosting image..."
docker image rm boosting

@echo "Removing boosting containers..."
@FOR /f "tokens=*" %%i IN ('docker ps -a -q --filter "ancestor=boosting"') DO docker container rm %%i

@echo "...done."
