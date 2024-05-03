#
# Copyright (C) 2017-2025 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://www.mozilla.org/en-US/MPL/2.0/
#

import stomp
import time
import calendar
import random
import json
import os
import sys


BROKER_SERVER=os.environ.get('BROKER_SERVER')
BROKER_PORT=int(os.environ.get('BROKER_PORT', 61610))
BROKER_USERNAME=os.environ.get('BROKER_USERNAME')
BROKER_PASSWORD=os.environ.get('BROKER_PASSWORD')
RETRY_CONNECT=os.environ.get('RETRY_CONNECT', 5)

TARGET_TOPIC='/topic/' + os.environ.get('TARGET_TOPIC', 'simple')
SEND_DELAY=int(os.environ.get('SEND_DELAY', 10))
VALUE_MIN=float(os.environ.get('VALUE_MIN', 0))
VALUE_MAX=float(os.environ.get('VALUE_MAX', 100))

class MyListener(stomp.ConnectionListener):
    def __init__(self, conn):
        self.conn = conn
    def on_error(self, frame):
        print("on_error %s %s", frame.headers, frame.body)
    def on_message(self, frame):
        print("Received ",frame.body)
    def on_disconnected(self):
        print('disconnected')

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

while True:
    try:
        print(f'Connecting to broker: {BROKER_SERVER}:{BROKER_PORT}, as user: {BROKER_USERNAME}...')
        sys.stdout.flush()
        conn = stomp.Connection([(BROKER_SERVER, BROKER_PORT)])
        conn.set_listener('', MyListener(conn))
        conn.connect(BROKER_USERNAME, BROKER_PASSWORD, wait=True)
        print('Connected')
        print(f'Sending messages to topic: {TARGET_TOPIC}, every {SEND_DELAY}s. Value range: [{VALUE_MIN}..{VALUE_MAX}]')
        sys.stdout.flush()

        while True:
            timestamp = calendar.timegm(time.gmtime())
#             message = '{ "metricValue": '+str(random.randint(VALUE_MIN, VALUE_MAX))+', "level": 1, "timestamp": '+str(timestamp)+' }'
            payload_body = {
                "metricValue": random.uniform(VALUE_MIN, VALUE_MAX),
                "level": 1,
                "timestamp": timestamp
            }
            message = json.dumps(payload_body)
            print(f"Sending {message}")
            sys.stdout.flush()
            conn.send(TARGET_TOPIC, message, headers={'type':'textMessage', 'amq-msg-type':'text'})

            time.sleep(SEND_DELAY)

    except:
        eprint("Error:", sys.exc_info()[0])
        eprint(f"Re-connect in {RETRY_CONNECT}s")
        sys.stderr.flush()
        time.sleep(RETRY_CONNECT)
