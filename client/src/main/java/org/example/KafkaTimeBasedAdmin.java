package org.example;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class KafkaTimeBasedAdmin {
    private static final Logger log = LoggerFactory.getLogger(KafkaTimeBasedProducer.class);

    public static void main(String[] args) {
        Properties config = new Properties();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        AdminClient admin = AdminClient.create(config);
        admin.deleteTopics(Collections.singletonList("time-events"));
        admin.close();
    }
}
