#
# BUILD:
# docker build -t <MY_IMAGE> -f .\tests\Dockerfile .
#
# RUN:
# docker run --rm -it <MY_IMAGE>
#

# -------------------------------------------------------------------------------------------------

# Use an official Ubuntu base image
FROM ubuntu:22.04

# Set environment variables to avoid interactive prompts during installation
ENV DEBIAN_FRONTEND=noninteractive
ENV USERNAME=ubuntu
ENV PASSWORD=ubuntu

# Install OpenSSH server and clean up
RUN apt-get update \
    && apt-get install -y iputils-ping telnet iproute2 python3-pip\
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Create the non-root user with the ability to set a password and authorized keys using environment variables
RUN useradd -ms /bin/bash $USERNAME && echo "$USERNAME:$PASSWORD" | chpasswd

# -------------------------------------------------------------------------------------------------

# ----- App1 -----
ENV JAVA_HOME=/home/$USERNAME/baguette-client/jre
ENV BROKER_SERVER=localhost
ENV BROKER_PORT=61616
ENV BROKER_USERNAME=aaa
ENV BROKER_PASSWORD=111
ENV TARGET_TOPIC=a__component_cpu__util__instance
ENV SEND_DELAY=10
ENV VALUE_MIN=0
ENV VALUE_MAX=100

COPY ./tests/app1  /app1
COPY ./tests/app2  /app2

WORKDIR /app2
USER $USERNAME
RUN pip3 install -r /app2/requirements.txt
# RUN chmod +x /app2/entrypoint.sh

ENTRYPOINT [ "/app2/entrypoint.sh" ]