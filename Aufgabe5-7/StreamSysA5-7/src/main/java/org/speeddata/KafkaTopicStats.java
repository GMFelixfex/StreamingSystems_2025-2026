package org.speeddata;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class KafkaTopicStats {
    public static long countMessages(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "topic-stats-consumer-");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("auto.offset.reset", "earliest");
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            List<TopicPartition> partitions = new ArrayList<>();
            consumer.partitionsFor(topic, Duration.ofSeconds(5)).forEach(pi ->
                    partitions.add(new TopicPartition(topic, pi.partition())));
            if (partitions.isEmpty()) return 0L;
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            List<Long> begins = new ArrayList<>();
            for (TopicPartition tp : partitions) {
                begins.add(consumer.position(tp));
            }
            consumer.seekToEnd(partitions);
            long total = 0L;
            for (TopicPartition tp : partitions) {
                long end = consumer.position(tp);
                int idx = partitions.indexOf(tp);
                long begin = begins.get(idx);
                total += Math.max(0L, end - begin);
            }
            return total;
        }
    }

    public static void printTopicCount(String bootstrapServers, String topic) {
        try {
            long cnt = countMessages(bootstrapServers, topic);
            System.out.println("Kafka topic '" + topic + "' message count (approx): " + cnt);
        } catch (Exception ex) {
            System.err.println("Failed to count messages for topic '" + topic + "': " + ex.getMessage());
        }
    }

    // For testing: delete and recreate the topic to clear all data.
    // This attempts to preserve the original partition count and replication factor.
    public static void clearTopic(String bootstrapServers, String topic) {
        System.out.println("Clearing topic before publishing test data...");
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient admin = AdminClient.create(props)) {
            try {
                var descF = admin.describeTopics(java.util.List.of(topic)).all();
                java.util.Map<String, TopicDescription> desc = descF.get();
                int partitions = 1;
                short replication = 1;
                if (desc.containsKey(topic)) {
                    partitions = desc.get(topic).partitions().size();
                    if (!desc.get(topic).partitions().isEmpty()) {
                        replication = (short) desc.get(topic).partitions().get(0).replicas().size();
                    }
                }

                System.out.println("Clearing topic '" + topic + "' (deleting and recreating)");
                admin.deleteTopics(java.util.List.of(topic)).all().get();
                // recreate topic with same partitions/replication
                NewTopic nt = new NewTopic(topic, partitions, replication);
                admin.createTopics(java.util.List.of(nt)).all().get();
                System.out.println("Topic '" + topic + "' cleared and recreated (partitions=" + partitions + ", replication=" + replication + ")");
            } catch (Exception e) {
                System.err.println("Failed to clear topic '" + topic + "': " + e.getMessage());
            }
        }
    }
}
