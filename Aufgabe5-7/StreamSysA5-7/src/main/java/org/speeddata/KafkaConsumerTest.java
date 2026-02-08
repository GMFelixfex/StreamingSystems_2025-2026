package org.speeddata;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerTest {
    public static void pollGroup(String bootstrapServers, String topic, String groupId, long millis) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.DoubleDeserializer");
        props.put("auto.offset.reset", "earliest");

        try (KafkaConsumer<String, Double> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            System.out.println("Test consumer subscribed with group.id=" + groupId + " polling for " + millis + "ms");
            long end = System.currentTimeMillis() + millis;
            long total = 0;
            while (System.currentTimeMillis() < end) {
                ConsumerRecords<String, Double> records = consumer.poll(Duration.ofMillis(500));
                int count = records.count();
                if (count > 0) {
                    total += count;
                    System.out.println("Test consumer polled " + count + " records (total=" + total + ")");
                }
            }
            System.out.println("Test consumer finished polling. total polled=" + total);
        } catch (Exception ex) {
            System.err.println("Test consumer failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
