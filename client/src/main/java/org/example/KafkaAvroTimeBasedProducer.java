package org.example;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.UUID;

public class KafkaAvroTimeBasedProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaAvroTimeBasedProducer.class);
    private static final String TOPIC = "time-events";

    private static final String SCHEMA_STRING = "{"
            + "\"type\":\"record\","
            + "\"name\":\"TimeEvent\","
            + "\"fields\":["
            + "{\"name\":\"id\", \"type\":\"string\"},"
            + "{\"name\":\"timestamp\", \"type\":\"long\"},"   // <-- Change type from string to long
            + "{\"name\":\"message\", \"type\":\"string\"}"
            + "]}";


    public static void main(String[] args) {
        Schema schema = new Schema.Parser().parse(SCHEMA_STRING);
        Properties props = new Properties();

        props.put("bootstrap.servers", "host.docker.internal:9093");
        props.put("key.serializer", KafkaAvroSerializer.class.getName());
        props.put("value.serializer", KafkaAvroSerializer.class.getName());
         props.put("schema.registry.url", "http://host.docker.internal:8081"); // Schema Registry URL

//        props.put("schema.registry.url", "https://host.docker.internal:8082"); // Use HTTPS, not HTTP
        props.put("basic.auth.credentials.source", "USER_INFO");
        props.put("basic.auth.user.info", "admin:admin-password"); // Replace with your actual credentials

        // TLS Configuration
        props.put("schema.registry.ssl.truststore.location", "E:\\projects\\aldrine\\kafka-sink-test\\infra\\truststore.jks");
        props.put("schema.registry.ssl.truststore.password", "truststore-password");
        props.put("schema.registry.ssl.truststore.type", "JKS");
        props.put("schema.registry.ssl.endpoint.identification.algorithm", "https");

        Producer<String, GenericRecord> producer = new KafkaProducer<>(props);

        for (int i = 0; i < 10; i++) {
            GenericRecord recordValue = new GenericData.Record(schema);
            recordValue.put("id", UUID.randomUUID().toString());
            recordValue.put("timestamp", System.currentTimeMillis()); // Store as long instead of String
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
    }
}
