package org.speeddata;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.Future;

public class KafkaFileProducer {
    private final KafkaProducer<String, Double> producer;
    private final String topic;

    public KafkaFileProducer(String bootstrap, String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrap);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.DoubleSerializer");
        // acks and retries can be tuned as needed
        props.put("acks", "all");
        this.producer = new KafkaProducer<String, Double>(props);
        this.topic = topic;
    }

    public void publishFile(String pathToFile) throws Exception {
        System.out.println("Publishing data from file to Kafka topic...");
        try (BufferedReader br = new BufferedReader(new FileReader(pathToFile))) {
            String line;
            System.out.println("Publishing data from file: " + pathToFile);
            long produced = 0L;
            final int LOG_EVERY = 100; // print progress every N records
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    SpeedEvent ev = SpeedEvent.parseFromString(trimmed);
                    // Use sensor id as key to allow grouping by partition if desired
                    String key = ev.getSensorId();
                    // get the speed as value
                    Double value = ev.getSpeed();
                    long timestamp = ev.getTimestamp().toEpochMilli();
                    ProducerRecord<String, Double> record = new ProducerRecord<>(topic, null, timestamp, key, value);
                    // Example output format: Producing record to topic speeddata: key=2, value=10.3, timestamp=1731514017024
                    Future<RecordMetadata> f = producer.send(record);
                    // optionally wait for send to complete to preserve order for this demo
                    f.get();
                    produced++;
                    if ((produced % LOG_EVERY) == 0) {
                        System.out.println("Produced " + produced + " records to topic '" + topic + "'...");
                    }
                } catch (IllegalArgumentException ex) {
                    // skip malformed lines
                    System.err.println("Skipping malformed line: " + ex.getMessage());
                }
            }
            System.out.println("Finished publishing data from file. Total produced=" + produced);
        }
        System.out.println("Data published.");
    }

    public void close() {
        producer.flush();
        producer.close();
    }
}
