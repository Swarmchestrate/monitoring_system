FROM python:3.13.1

# ---------------------------------------------------------
#  Install SSH server and configure 'ubuntu' user
# ---------------------------------------------------------

# Install OpenSSH server and sudo
#RUN apt-get update && apt-get install -y openssh-server sudo

# Create SSH directory
#RUN mkdir /var/run/sshd

# Create a new user 'ubuntu' with password 'ubuntu' and add to sudo group
# RUN useradd -m -s /bin/bash ubuntu && \
#     echo 'ubuntu:ubuntu' | chpasswd && \
#     usermod -aG sudo ubuntu

# Allow passwordless sudo for the 'ubuntu' user
# RUN echo 'ubuntu ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

# Allow root login and adjust SSH configuration to allow the 'ubuntu' user
# RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
#     sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# Expose port 22 for SSH
#EXPOSE 22

# ---------------------------------------------------------
#  Install prometheus app
# ---------------------------------------------------------

COPY . /app
WORKDIR /app

RUN pip install -r requirements.txt

ENTRYPOINT ["bash", "-c", "python3 brokerapp.py"]