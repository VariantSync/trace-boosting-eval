FROM --platform=linux/amd64 openjdk:23-slim

# Set the environment variable to noninteractive to avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive
# Update the package list and upgrade the system without any prompts
RUN apt-get update && apt-get upgrade -y
# Install Maven without any prompts
RUN apt-get install -y maven


WORKDIR /home/user
COPY src ./src
COPY python ./python
COPY local-maven-repo ./local-maven-repo
COPY pom.xml ./pom.xml

RUN mvn clean package || exit


FROM --platform=linux/amd64 openjdk:23-slim

# Set the environment variable to noninteractive to avoid interactive prompts
ENV DEBIAN_FRONTEND=noninteractive
# Update the package list and upgrade the system without any prompts
RUN apt-get update && apt-get upgrade -y

RUN apt-get install bash git python3 unzip curl -y

# Install Python and pip

RUN apt-get install -y python3 python3-pip python3-virtualenv

ARG GROUP_ID
ARG USER_ID

RUN if ! getent group $GROUP_ID; then \
      groupadd -g $GROUP_ID user; \
    fi
RUN useradd --no-create-home --uid $USER_ID --gid $GROUP_ID --home-dir /home/user --shell /bin/bash user
RUN mkdir -p /home/user
RUN chown $USER_ID:$GROUP_ID /home/user
WORKDIR /home/user

# Copy the docker resources
COPY docker/* ./
COPY python ./python
RUN mkdir data
COPY data ./data 
COPY setup.sh ./setup.sh

# Copy all relevant files from the previous stage
COPY --from=0 /home/user/target* ./

# Set up the python environment for executing the plot creation
WORKDIR /home/user/python
WORKDIR /home/user

# Adjust permissions
RUN chown $USER_ID:$GROUP_ID /home/user -R
RUN chmod +x run-experiments.sh
RUN chmod +x entrypoint.sh
RUN chmod +x setup.sh

ENTRYPOINT ["./entrypoint.sh", "./run-experiments.sh"]
USER user
