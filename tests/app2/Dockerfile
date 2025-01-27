FROM python:3.13.1

COPY . /app
WORKDIR /app

RUN chmod +x entrypoint.sh && \
    pip install -r requirements.txt

ENTRYPOINT ["/app/entrypoint.sh"]