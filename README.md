# kafka-sink-test


docker-compose down -v --remove-orphans
docker system prune -a --volumes
docker-compose up


curl -X DELETE http://localhost:8083/connectors/cassandra-sink
curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
curl -X POST -H "Content-Type: application/json" --data @../kafka-sink/cassandra-sink.json http://localhost:8083/connectors | jq .
curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
curl -X PUT -H "Content-Type: application/json" --data @../kafka-sink/cassandra-sink.json http://localhost:8083/connectors | jq .


curl -X PUT http://localhost:8083/connectors/cassandra-sink/config \
-H "Content-Type: application/json" \
-d '{
"connector.class": "com.datastax.oss.kafka.sink.CassandraSinkConnector",
"tasks.max": "1",
"topics": "time-events",
"cassandra.contact.points": "cassandra",
"cassandra.port": "9042",
"cassandra.local.dc": "datacenter1",
"cassandra.keyspace": "kafka_sink",
"cassandra.table": "events",
"cassandra.consistency.level": "LOCAL_QUORUM",
"topic.time-events.kafka_sink.events.mapping": "id=value.id, event_time=value.timestamp, message=value.message",
"topic.time-events.kafka_sink.events.deletesEnabled": "false"
}'




curl -s http://localhost:8083/connector-plugins | jq .


curl -X POST http://localhost:8083/connectors/cassandra-sink/config \
-H "Content-Type: application/json" \
-d '{
"connector.class": "com.datastax.oss.kafka.sink.CassandraSinkConnector",
"tasks.max": "1",
"topics": "time-events",
"contactPoints": "cassandra",
"loadBalancing.localDc": "datacenter1",
"port": 9042,
"ignoreErrors": "None",
"maxConcurrentRequests": 500,
"maxNumberOfRecordsInBatch": 32,
"queryExecutionTimeout": 30,
"connectionPoolLocalSize": 4,
"jmx": true,
"compression": "None",
"auth.provider": "None",
"ssl.provider": "None",
"ssl.hostnameValidation": true,
"topic.time-events.kafka_sink.events.mapping": "id=value.id, event_time=value.timestamp, message=value.message",
"topic.time-events.kafka_sink.events.consistencyLevel": "LOCAL_QUORUM",
"topic.time-events.kafka_sink.events.ttl": -1,
"topic.time-events.kafka_sink.events.ttlTimeUnit": "SECONDS",
"topic.time-events.kafka_sink.events.timestampTimeUnit": "MICROSECONDS",
"topic.time-events.kafka_sink.events.nullToUnset": "true",
"topic.time-events.kafka_sink.events.deletesEnabled": "false",
"topic.time-events.codec.locale": "en_US",
"topic.time-events.codec.timeZone": "UTC",
"topic.time-events.codec.timestamp": "UNIX_EPOCH",
"topic.time-events.codec.unit": "MILLISECONDS"
"topic.time-events.codec.date": "ISO_LOCAL_DATE",
"topic.time-events.codec.time": "ISO_LOCAL_TIME",
}'


docker exec -it cassandra cqlsh -e "SELECT COUNT(*) FROM kafka_sink.events;"


docker exec -it $(docker ps --filter name=cassandra --format "{{.ID}}") cqlsh


docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --delete --topic time-events


docker exec -it infra-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 \
--group connect-cassandra-sink \
--reset-offsets --to-earliest \
--execute --all-topics

docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --list