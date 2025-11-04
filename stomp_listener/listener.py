import time
import stomp

class MyListener(stomp.ConnectionListener):
    def on_connected(self, frame):
        print("✅ Connected to ActiveMQ")

    def on_error(self, frame):
        print("❌ Received an error:", frame.body)

    def on_message(self, frame):
        destination = frame.headers.get('destination', '<unknown>')
        print(f"📩 Received a message from [{destination}]: {frame.body}")


# Broker details
host = 'emsserver-ems-server'       # the service name or your broker IP
port = 61610                        # default STOMP port
username = ''                       #insert broker's credentials
password = ''                       #insert broker's credentials
destination = '/topic/>'            # or /topic/your-topic for pub-sub


conn = stomp.Connection([(host, port)])
conn.set_listener('', MyListener())

conn.connect(login=username, passcode=password, wait=True)

conn.subscribe(destination=destination, id=1, ack='auto')

while True:
    time.sleep(20)

conn.disconnect()
