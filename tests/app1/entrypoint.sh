#!/usr/bin/env bash
#
# Copyright (C) 2017-2025 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://www.mozilla.org/en-US/MPL/2.0/
#

export JAVA_HOME=/home/ubuntu/baguette-client/jre
export PATH=$JAVA_HOME/bin:$PATH
export TARGET_TOPIC=a__component_cpu__util__instance

# Start simple-app to send messages to broker
java -jar simple-app-java-jar-with-dependencies.jar
