#
# Copyright (C) 2017-2025 Institute of Communication and Computer Systems (imu.iccs.gr)
#
# This Source Code Form is subject to the terms of the Mozilla Public License, v2.0.
# If a copy of the MPL was not distributed with this file, You can obtain one at
# https://www.mozilla.org/en-US/MPL/2.0/
#

from prometheus_client import start_http_server, Counter, Summary
import random
import time

# Port of Prometheus endpoint
PORT = 9000

# Create a metric to count requests made
REQUEST_COUNT = Counter( 'request_count', 'Number of requests')

# Create a metric to track time spent and requests made.
REQUEST_TIME = Summary('request_processing_seconds', 'Time spent processing request')

# Decorate function with metric.
@REQUEST_TIME.time()
def process_request(t):
    """A dummy function that takes some time."""
    time.sleep(t)
    REQUEST_COUNT.inc()

if __name__ == '__main__':
    # Start up the server to expose the metrics.
    start_http_server(PORT)
    print(f'Exposing Prometheus metrics at port: {PORT}')
    # Generate some requests.
    while True:
        process_request(5 + 5 * random.random())