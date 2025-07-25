#
# Copyright (C) 2017-2025 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0, unless 
# Esper library is used, in which case it is subject to the terms of General Public License v2.0.
# If a copy of the MPL was not distributed with this file, you can obtain one at 
# https://www.mozilla.org/en-US/MPL/2.0/
#


# ----------------- EMS Builder image -----------------
FROM docker.io/library/maven:3.9.6-eclipse-temurin-21 AS ems-server-builder
ENV BASEDIR=/app
WORKDIR ${BASEDIR}
COPY ems-main/ems-core           ${BASEDIR}/ems-core
RUN --mount=type=cache,target=/root/.m2  mvn -f ${BASEDIR}/ems-core/pom.xml -DskipTests clean install -P '!build-docker-image'
COPY ems-swarmchestrate/ems-swarmchestrate   ${BASEDIR}/ems-swarmchestrate
RUN --mount=type=cache,target=/root/.m2  mvn -f ${BASEDIR}/ems-swarmchestrate/pom.xml -DskipTests clean install -P '!build-docker-image'
RUN cp ems-core/control-service/target/control-service.jar . && \
    java -Djarmode=layertools -jar control-service.jar extract


# -----------------   EMS-Core Run image   -----------------
FROM eclipse-temurin:21.0.3_9-jre AS ems-server-core

# Install required and optional packages
RUN wget --progress=dot:giga -O /usr/local/bin/dumb-init \
          https://github.com/Yelp/dumb-init/releases/download/v1.2.5/dumb-init_1.2.5_x86_64 && \
    chmod +x /usr/local/bin/dumb-init
#RUN apt-get update \
#    && apt-get install -y netcat vim iputils-ping \
#    && rm -rf /var/lib/apt/lists/*

# Add an EMS user
ARG EMS_USER=emsuser
ARG EMS_HOME=/opt/ems-server
RUN mkdir ${EMS_HOME} && \
    addgroup ${EMS_USER} && \
    adduser --home ${EMS_HOME} --no-create-home --ingroup ${EMS_USER} --disabled-password ${EMS_USER} && \
    chown ${EMS_USER}:${EMS_USER} ${EMS_HOME}

USER ${EMS_USER}
WORKDIR ${EMS_HOME}

# Setup environment
ENV BASEDIR=${EMS_HOME}
ENV EMS_CONFIG_DIR=${BASEDIR}/config

ENV BIN_DIR=${BASEDIR}/bin
ENV CONFIG_DIR=${BASEDIR}/config
ENV LOGS_DIR=${BASEDIR}/logs
ENV PUBLIC_DIR=${BASEDIR}/public_resources

# Download a JRE suitable for running EMS clients, and
# offer it for download
ENV JRE_LINUX_PACKAGE=zulu21.34.19-ca-jre21.0.3-linux_x64.tar.gz
RUN mkdir -p ${PUBLIC_DIR}/resources && \
    wget --progress=dot:giga -O ${PUBLIC_DIR}/resources/${JRE_LINUX_PACKAGE} https://cdn.azul.com/zulu/bin/${JRE_LINUX_PACKAGE}

# Copy resource files to image
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/ems-core/bin              ${BIN_DIR}
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/ems-core/config-files     ${CONFIG_DIR}
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/ems-core/public_resources ${PUBLIC_DIR}

# Create 'logs', and 'models' directories. Make bin/*.sh scripts executable
RUN mkdir ${LOGS_DIR} && \
    chmod +rx ${BIN_DIR}/*.sh && \
    mkdir -p ${EMS_HOME}/models

# Copy files from builder container
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/dependencies          ${BASEDIR}
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/spring-boot-loader    ${BASEDIR}
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/snapshot-dependencies ${BASEDIR}
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/application           ${BASEDIR}

# Copy ESPER dependencies
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder /app/ems-core/control-service/target/esper*.jar       ${BASEDIR}/BOOT-INF/lib/

EXPOSE 2222
EXPOSE 8111
EXPOSE 61610
EXPOSE 61616
EXPOSE 61617

ENTRYPOINT ["dumb-init", "./bin/run.sh"]

# -----------------   EMS-Swarmchestrate Runtime image   -----------------
FROM ems-server-core AS ems-server-swarmchestrate

RUN date > /tmp/BUILD-TIME

COPY --from=ems-server-builder /app/ems-swarmchestrate/target/ems-swarmchestrate-plugin-1.0.0-SNAPSHOT.jar /plugins/
#COPY --from=ems-server-builder /app/ems-nebulous/models   ${BASEDIR}/models
ENV EXTRA_LOADER_PATHS=/plugins/*
ENV SCAN_PACKAGES=eu.swarmchestrate.ems


# -----------------   EMS-Client Runtime image   -----------------
FROM eclipse-temurin:21.0.3_9-jre AS ems-client

# Install required and optional packages
#RUN apt-get update \
#    && apt-get install -y vim iputils-ping \
#    && rm -rf /var/lib/apt/lists/*

# Add an EMS user
ARG EMS_USER=emsuser
ARG EMS_HOME=/opt/baguette-client
RUN mkdir -p ${EMS_HOME} && \
    addgroup ${EMS_USER} && \
    adduser --home ${EMS_HOME} --no-create-home --ingroup ${EMS_USER} --disabled-password ${EMS_USER} && \
    chown ${EMS_USER}:${EMS_USER} ${EMS_HOME}

USER ${EMS_USER}
WORKDIR ${EMS_HOME}

# Setup environment
ARG EMS_CONFIG_DIR=${EMS_HOME}/conf
ARG JAVA_HOME=/opt/java/openjdk
ARG PATH=$JAVA_HOME/bin:$PATH
ARG INSTALLATION_PACKAGE=baguette-client-installation-package.tgz

# Copy Baguette Client files
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder  /app/ems-core/baguette-client/target/$INSTALLATION_PACKAGE  /tmp
RUN tar zxvf /tmp/$INSTALLATION_PACKAGE -C /opt && rm -f /tmp/$INSTALLATION_PACKAGE
COPY --chown=${EMS_USER}:${EMS_USER} --from=ems-server-builder  /app/ems-core/baguette-client/conf/* ${EMS_HOME}/conf/

EXPOSE 61610
EXPOSE 61616
EXPOSE 61617

ENTRYPOINT ["/bin/sh", "-c", "/opt/baguette-client/bin/run.sh  &&  tail -f /opt/baguette-client/logs/output.txt"]