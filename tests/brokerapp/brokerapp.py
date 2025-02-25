import pika
import time

class MyListener:
    def __init__(self, file_path):
        self.file_path = file_path

    def on_message(self, channel, method, properties, body):
        message = body.decode()
        print(f'Message received: {message}')
        with open(self.file_path, 'a') as f:
            f.write(f'Message received: {message}\n')
        channel.basic_ack(delivery_tag=method.delivery_tag)

def main():
    file_path = '/app/output_file.txt'  # Replace with your desired file path
    # Ensure the output file exists and is empty at the start
    with open(file_path, 'w') as f:
        f.write('')

    connection_params = pika.ConnectionParameters(host='ems', port=61610, credentials=pika.PlainCredentials('aaa', '111'))
    connection = pika.BlockingConnection(connection_params)
    channel = connection.channel()

    channel.exchange_declare(exchange='topic_exchange', exchange_type='topic')
    result = channel.queue_declare(queue='', exclusive=True)
    queue_name = result.method.queue

    channel.queue_bind(exchange='topic_exchange', queue=queue_name, routing_key='cpu_util_instance')

    listener = MyListener(file_path)
    channel.basic_consume(queue=queue_name, on_message_callback=listener.on_message)

    print('Waiting for messages. To exit press CTRL+C')
    try:
        channel.start_consuming()
    except KeyboardInterrupt:
        channel.stop_consuming()
    connection.close()

if __name__ == '__main__':
    main()
