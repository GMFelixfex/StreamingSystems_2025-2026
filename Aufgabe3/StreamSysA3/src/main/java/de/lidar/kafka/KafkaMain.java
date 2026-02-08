package de.lidar.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.lidar.model.LidarPoint;
import de.lidar.util.ScanTag;
import de.lidar.util.UtilFunctions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.*;

public class KafkaMain {
    private static final String BOOTSTRAP = "localhost:9092";
    // Volatile running flag to control the consumer thread, guarantees visibility across threads
    private volatile boolean running = true;

    public static void main(String[] args) throws Exception {
        new KafkaMain().run();
    }

    public void run() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Producer (used to forward/tag and emit distances)
        Properties prodProps = new Properties();
        prodProps.put("bootstrap.servers", BOOTSTRAP);
        prodProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        prodProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer<String, String> producer = new KafkaProducer<>(prodProps);

        // Consumer (single consumer for all three topics)
        Properties consProps = new Properties();
        consProps.put("bootstrap.servers", BOOTSTRAP);
        consProps.put("group.id", "lidar-processor-group");
        consProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consProps.put("auto.offset.reset", "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consProps);
        consumer.subscribe(Arrays.asList("Scans", "TaggedScans", "Distanzen"));

        // Shared processor state
        ScanTag scanTag = new ScanTag();
        Map<Integer, LidarPoint> lastPointByScan = new HashMap<>();
        TreeMap<Integer, Double> totals = new TreeMap<>();

        // Consumer thread
        Thread consThread = new Thread(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, String> r : records) {
                        String topic = r.topic();
                        try {
                            if ("Scans".equals(topic)) {
                                // Parse, tag, forward to TaggedScans
                                LidarPoint p = mapper.readValue(r.value(), LidarPoint.class);
                                p = scanTag.Check(p);
                                String out = mapper.writeValueAsString(p);
                                producer.send(new ProducerRecord<>("TaggedScans", out));
                            } else if ("TaggedScans".equals(topic)) {
                                LidarPoint p = mapper.readValue(r.value(), LidarPoint.class);
                                if (p.qualitaet < 15) continue;
                                int scan = p.scanNr;
                                LidarPoint last = lastPointByScan.get(scan);
                                if (last != null && last.scanNr == p.scanNr) {
                                    double d = UtilFunctions.calcDistance(last, p);
                                    Map<String, Object> msg = new HashMap<>();
                                    msg.put("scan", scan);
                                    msg.put("dist", d);
                                    String out = mapper.writeValueAsString(msg);
                                    producer.send(new ProducerRecord<>("Distanzen", out));
                                }
                                lastPointByScan.put(scan, p);
                            } else if ("Distanzen".equals(topic)) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> m = mapper.readValue(r.value(), Map.class);
                                int scan = (Integer) ((Number) m.get("scan")).intValue();
                                double dist = ((Number) m.get("dist")).doubleValue();
                                totals.put(scan, totals.getOrDefault(scan, 0.0) + dist);
                                try {
                                    UtilFunctions.writeResults(totals,"resultsKafka.txt");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } finally {
                consumer.close();
            }
        }, "kafka-consumer-thread");

        consThread.start();

        // Produce Scans from file
        File file = new File("data/Lidar-scans.txt");
        if (!file.exists()) {
            System.out.println("data/Lidar-scans.txt not found; skipping production.");
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.contains("winkel")) continue;
                    LidarPoint p = UtilFunctions.parseLine(line);
                    String out = mapper.writeValueAsString(p);
                    producer.send(new ProducerRecord<>("Scans", out));
                }
                System.out.println("Finished producing Scans to Kafka.");
            }
        }

        System.out.println("Processor running. Press Enter to stop...");
        System.in.read();
        running = false;
        // wake consumer by closing; join thread
        consThread.join(2000);
        producer.close();
        System.out.println("Stopped.");
    }
}