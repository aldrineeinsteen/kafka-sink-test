package org.example;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.UUID;

public class KafkaTimeBasedProducer {
    private static final Logger log = LoggerFactory.getLogger(KafkaTimeBasedProducer.class);

    public static void main(String[] args) {
        String topic = "time-events";
        Properties props = new Properties();

        props.put("bootstrap.servers", "host.docker.internal:9093");
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());

        Producer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 0; i < 10; i++) {
            String key = UUID.randomUUID().toString();
            String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(System.currentTimeMillis()));
            String message = String.format(
                    "{\"id\": \"%s\", \"timestamp\": \"%s\", \"message\": \"Test message %d\"}",
                    key, timestamp, i
            );

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);

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
