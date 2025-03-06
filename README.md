# Kafka Sink Test - Quick Start Guide

## Prerequisites
Ensure you have the following installed on your system:
- Docker
- Docker Compose
- `jq` (for JSON parsing in CLI requests)

## Setup and Running the Environment

1. **Clean up any existing containers, volumes, and networks:**
   ```sh
   docker-compose down -v --remove-orphans
   docker system prune -a --volumes
   ```

2. **Start the Kafka infrastructure with Docker Compose:**
   ```sh
   docker-compose up -d
   
   docker exec -it $(docker ps --filter name=cassandra --format "{{.ID}}") cqlsh
    
   CREATE KEYSPACE kafka_sink WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

    USE kafka_sink;

    CREATE TABLE events (
    id UUID PRIMARY KEY,
    event_time TIMESTAMP,
    message TEXT
    ) WITH compaction = { 'class': 'TimeWindowCompactionStrategy', 'compaction_window_size': '1', 'compaction_window_unit': 'DAYS' };

   ```

## Kafka Connect Setup

3. **Delete existing Cassandra Sink connector (if any):**
   ```sh
   curl -X DELETE http://localhost:8083/connectors/cassandra-sink
   ```

4. **Check the status of the Cassandra Sink connector:**
   ```sh
   curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
   ```

5. **Deploy the Cassandra Sink connector:**
   ```sh
   curl -X POST -H "Content-Type: application/json" --data @../kafka-sink/cassandra-sink.json http://localhost:8083/connectors | jq .
   ```

6. **Verify the status of the newly created Cassandra Sink connector:**
   ```sh
   curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
   ```

7. **Update the existing Cassandra Sink connector configuration:**
   ```sh
   curl -X PUT -H "Content-Type: application/json" --data @../kafka-sink/cassandra-sink.json http://localhost:8083/connectors | jq .
   ```

## Verify Available Connector Plugins

8. **Check available Kafka Connector plugins:**
   ```sh
   curl -s http://localhost:8083/connector-plugins | jq .
   ```

## Register Cassandra Sink Connector with Custom Configuration

9. **Manually configure and register the Cassandra Sink Connector:**
   ```sh
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
     "topic.time-events.codec.unit": "MILLISECONDS",
     "topic.time-events.codec.date": "ISO_LOCAL_DATE",
     "topic.time-events.codec.time": "ISO_LOCAL_TIME"
   }'
   ```

## Validate Data in Cassandra

10. **Check the number of records in Kafka Sink table:**
    ```sh
    docker exec -it cassandra cqlsh -e "SELECT COUNT(*) FROM kafka_sink.events;"
    ```

11. **Access Cassandra CQL shell:**
    ```sh
    docker exec -it $(docker ps --filter name=cassandra --format "{{.ID}}") cqlsh
    ```

## Kafka Topic and Consumer Management

12. **Delete the `time-events` Kafka topic:**
    ```sh
    docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --delete --topic time-events
    ```

13. **Reset offsets for the consumer group `connect-cassandra-sink` to earliest:**
    ```sh
    docker exec -it infra-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 \
    --group connect-cassandra-sink \
    --reset-offsets --to-earliest \
    --execute --all-topics
    ```

14. **List all Kafka topics:**
    ```sh
    docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --list
    ```

---
## **Final Notes**
- Ensure that **Cassandra, Kafka, and Kafka Connect** are running properly before testing.
- If any issue arises, check the **logs** using:
  ```sh
  docker-compose logs -f
  ```
- Use `docker ps` to confirm all required containers are running.

Happy Streaming! 🚀



---

# Kafka Sink Test - Project Set up

## Overview
This project sets up an **Apache Kafka** environment with **Confluent Schema Registry** and a **Cassandra Sink Connector** to process and store streaming data. It includes a **Kafka Avro Producer** to send messages in Avro format and integrates a **Cassandra database** for persistence.

## Directory Structure
```
/mnt/e/projects/aldrine/kafka-sink-test
│── LICENSE
│── README.md
│── client  # Contains the Kafka Producer implementation
│   ├── pom.xml
│   ├── src
│   │   ├── main/java/org/example
│   │   │   ├── KafkaAvroTimeBasedProducer.java
│   │   │   ├── KafkaTimeBasedAdmin.java
│   │   │   ├── KafkaTimeBasedProducer.java
│   │   ├── test/java/org/example
│   │   │   ├── AppTest.java
│   ├── target  # Compiled artifacts
│── infra  # Infrastructure setup with Docker
│   ├── docker-compose.yml
│   ├── truststore.jks
│   ├── keystore.jks
│   ├── kafka-plugins  # Plugins for Kafka
│   │   ├── confluent  # Avro and Schema Registry JARs
│   │   ├── datastax-cassandra  # Cassandra Sink Connector
│── kafka-sink  # Configuration for the Cassandra Sink Connector
│   ├── cassandra-sink.json
```

---

## Setup & Execution

### 1. **Clean up and start the environment**
```sh
docker-compose down -v --remove-orphans
docker system prune -a --volumes
docker-compose up -d  # Runs Kafka, Schema Registry, and Cassandra in the background
```

### 2. **Deploy the Cassandra Sink Connector**
```sh
curl -X DELETE http://localhost:8083/connectors/cassandra-sink
curl -X POST -H "Content-Type: application/json" --data @../kafka-sink/cassandra-sink.json http://localhost:8083/connectors | jq .
curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
```

### 3. **Check Kafka Topics & Consumer Groups**
```sh
docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --list
docker exec -it infra-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 --group connect-cassandra-sink --reset-offsets --to-earliest --execute --all-topics
```

### 4. **Check Cassandra Data**
```sh
docker exec -it cassandra cqlsh -e "SELECT COUNT(*) FROM kafka_sink.events;"
```

---

## Kafka Avro Producer - Code Explanation

### **`KafkaAvroTimeBasedProducer.java`**
This is a **Kafka Producer** that sends Avro-encoded messages to the `time-events` topic.

#### **Key Components:**
- **Defines an Avro schema** for `id`, `timestamp`, and `message`.
- **Uses KafkaAvroSerializer** to serialize Avro records.
- **Sends messages** asynchronously to Kafka.

#### **Code Breakdown:**
```java
Schema schema = new Schema.Parser().parse(SCHEMA_STRING);
Properties props = new Properties();
props.put("bootstrap.servers", "host.docker.internal:9093");
props.put("key.serializer", KafkaAvroSerializer.class.getName());
props.put("value.serializer", KafkaAvroSerializer.class.getName());
props.put("schema.registry.url", "http://host.docker.internal:8081");
```
- Connects to Kafka at `9093`.
- Uses **Confluent Schema Registry** at `8081` for Avro serialization.

```java
Producer<String, GenericRecord> producer = new KafkaProducer<>(props);
for (int i = 0; i < 10; i++) {
    GenericRecord recordValue = new GenericData.Record(schema);
    recordValue.put("id", UUID.randomUUID().toString());
    recordValue.put("timestamp", System.currentTimeMillis());
    recordValue.put("message", "Test message " + i);

    ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(TOPIC, recordValue.get("id").toString(), recordValue);
    producer.send(record, (metadata, exception) -> {
        if (exception != null) {
            log.error("Error sending message: {}", exception.getMessage(), exception);
        } else {
            log.info("Sent message to {} partition {}", metadata.topic(), metadata.partition());
        }
    });
    producer.flush();
}
producer.close();
```
- **Generates random events** with timestamps and messages.
- **Sends events** to the `time-events` Kafka topic.
- **Handles errors and logs successful sends.**

#### **Fixing Schema Registry Issues**
- The `timestamp` field was changed from **String to long**, causing incompatibility.
- Use **schema compatibility settings** to allow evolution:
```sh
curl -X PUT -H "Content-Type: application/json" \
--data '{"compatibility": "FORWARD"}' \
http://localhost:8081/config/time-events-value
```

---

## Frequently Used Commands

### **Kafka Topic Management**
```sh
docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --list
docker exec -it infra-kafka-1 kafka-topics --bootstrap-server kafka:9092 --delete --topic time-events
```

### **Schema Registry**
```sh
curl -X GET http://localhost:8081/subjects/time-events-value/versions | jq .
curl -X DELETE http://localhost:8081/subjects/time-events-value/versions/1
```

### **Cassandra Connector**
```sh
curl -X GET http://localhost:8083/connector-plugins | jq .
curl -X GET http://localhost:8083/connectors/cassandra-sink/status | jq .
```

### **Reset Consumer Group Offsets**
```sh
docker exec -it infra-kafka-1 kafka-consumer-groups --bootstrap-server kafka:9092 \
--group connect-cassandra-sink \
--reset-offsets --to-earliest \
--execute --all-topics
```

---

## Summary
- **Sets up Kafka, Schema Registry, and Cassandra** using Docker Compose.
- **Sends Avro messages** using `KafkaAvroTimeBasedProducer`.
- **Streams messages to Cassandra** using the **Cassandra Sink Connector**.
- **Handles Schema Evolution** via Schema Registry.

---

✅ **Project is ready to stream data to Cassandra using Kafka!** 🚀

