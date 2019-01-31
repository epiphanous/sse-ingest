# sse-ingest
Ingest a server-sent events source into a distributed log. Supported distributed logs include kafka and kinesis.

## Configuration

Configuration is by environment variables.

| Variable                            | Default                                          | Usage                                                  |
|-------------------------------------|--------------------------------------------------|--------------------------------------------------------|
| EVENT_SOURCE_NAME                   | test-events                                      | Default name for topic/stream                          |
| EVENT_SOURCE_URL                    | http://localhost:4000/events                     | The url of the SSE source                              |
| EVENT_SOURCE_PAYLOAD_TYPE           | data                                             | Event type (in event source parlance)                  |
| EVENT_SOURCE_PAYLOAD_ID             | id                                               | Top level key in the json payload holding the event ID |
| SINK_TYPE                           | kakfa                                            | kafka or kinesis                                       |
| KAFKA_TOPIC                         | ${EVENT_SOURCE_NAME}                             | name of kafka topic to write to                        |
| KAFKA_BOOTSTRAP_SERVERS             | localhost:9092                                   | kakfa bootstrap servers (comma-separated for multiple) |
| KINESIS_REGION                      | us-east-1                                        | The AWS region for the kinesis endpoint                |
| KINESIS_ENDPOINT                    | https://kinesis.${KINESIS_REGION}.aws.amazon.com | The kinesis endpoint                                   |
| KINESIS_STREAM                      | ${EVENT_SOURCE_NAME}                             | name of kinesis stream to write to                     |
| KINESIS_FLOW_PARALLELISM            | 1                                                | number of parallel kinesis producers                   |
| KINESIS_FLOW_MAX_BATCH_SIZE         | 500                                              | max number of records to batch                         |
| KINESIS_FLOW_MAX_RECORDS_PER_SECOND | 1000                                             | max records written per sec                            |
| KINESIS_FLOW_MAX_BYTES_PER_SECOND   | 1000000                                          | max bytes written per sec                              |
| KINESIS_FLOW_MAX_RETRIES            | 5                                                | max retries after recoverable errors                   |
| KINESIS_FLOW_BACKOFF_STRATEGY       | exponential                                      | backoff strategy on retries (exponential or linear)    |
| KINESIS_FLOW_RETRY_INITIAL_TIMEOUT  | 100                                              | initial delay between retries (in milliseconds)        |

Defaults to reading from a local event source and writing to a local kafka topic called test-events.
If using Kinesis, you can optimize flow parameters, but the defaults are sensible.

## Usage

To run both a kafka server and `sse-ingest`:
```
docker-compose up -d
```

To run just `sse-ingest` (if you want to write to kinesis):
```js
docker-compose up ingest -d
```



