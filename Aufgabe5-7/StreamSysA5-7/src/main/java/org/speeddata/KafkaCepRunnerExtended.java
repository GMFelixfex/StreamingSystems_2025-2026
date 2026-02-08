package org.speeddata;

import com.espertech.esper.client.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.DoubleDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Properties;

public class KafkaCepRunnerExtended {

    private static final boolean OUTPUT_TO_FILE = true;
    private static final String OUTPUT_FILE = "data/esper_averages.txt";
    public static void run(String bootstrap, String topic) {
        System.out.println("--- Starting Extended Esper CEP Processing ---");

        // Esper Setup
        Configuration config = new Configuration();
        config.addEventType("SpeedEvent", SpeedEvent.class.getName());
        config.addEventType("AverageSpeedEvent", AverageSpeedEvent.class.getName());
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        EPAdministrator admin = epService.getEPAdministrator();
        EPRuntime runtime = epService.getEPRuntime();

        // ANFRAGE 1: Durchschnitt berechnen und als AverageSpeedEvent "veröffentlichen"
        String avgEpl = "insert into AverageSpeedEvent " +
                "select sensorId, avg(speed) * 3.6 as avgSpeedKmH, max(timestamp.toEpochMilli()) as maxTs " +
                "from SpeedEvent(speed >= 0).win:ext_timed_batch(timestamp.toEpochMilli(), 10 sec) " +
                "group by sensorId "+
                "having max(timestamp.toEpochMilli()) is not null";
        admin.createEPL(avgEpl);

        // ANFRAGE 2: Komplexes Ereignis (Pattern)
        // Wir suchen: Ein Event 'a', gefolgt von einem Event 'b' des GLEICHEN Sensors,
        // bei dem die Differenz der Geschwindigkeiten > 20 ist.
        String patternEpl = "select a.sensorId as sId, a.avgSpeedKmH as oldS, b.avgSpeedKmH as newS, " +
                "(b.avgSpeedKmH - a.avgSpeedKmH) as diff " +
                "from pattern [every a=AverageSpeedEvent -> " +
                "b=AverageSpeedEvent(sensorId = a.sensorId and Math.abs(avgSpeedKmH - a.avgSpeedKmH) > 20)]";

        // Listener für das komplexe Ereignis
        EPStatement alertStatement = admin.createEPL(patternEpl);
        alertStatement.addListener((newEvents, oldEvents) -> {
            if (newEvents != null) {
                for (EventBean event : newEvents) {
                    double diff = (double) event.get("diff");
                    String type = diff > 0 ? "BESCHLEUNIGUNG" : "STAUGEFAHR";
                    System.err.printf("!!! COMPLEX EVENT: %s an Sensor %s! Speed changed from %.2f to %.2f (Delta: %.2f km/h)%n",
                            type, event.get("sId"), event.get("oldS"), event.get("newS"), diff);
                }
            }
        });

        // Listener für die Normalen Durchschnittsergebnisse
        EPStatement avgStatement = admin.createEPL(avgEpl); // Wir brauchen das auch als Statement, um die Listener zu registrieren
        avgStatement.addListener((newEvents, oldEvents) -> {
            if (newEvents != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.000").withZone(ZoneId.of("UTC"));
                for (EventBean event : newEvents) {
                    long maxTs = (long) event.get("maxTs");
                    long start = (maxTs / 10000) * 10000;
                    if (OUTPUT_TO_FILE) {
                        String line = String.format("[%s, %s] SensorId=%s AverageSpeed=%.2f%n",
                                fmt.format(Instant.ofEpochMilli(start)),
                                fmt.format(Instant.ofEpochMilli(start + 10000)),
                                event.get("sensorId"), event.get("avgSpeedKmH"));
                        try {
                            java.nio.file.Files.write(java.nio.file.Paths.get(OUTPUT_FILE), line.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        System.out.printf("[%s, %s] SensorId=%s AverageSpeed=%.2f%n",
                                fmt.format(Instant.ofEpochMilli(start)),
                                fmt.format(Instant.ofEpochMilli(start + 10000)),
                                event.get("sensorId"), event.get("avgSpeedKmH"));
                    }
                }
            }
        });


        // Kafka Consumer
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "esper-consumer-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, DoubleDeserializer.class.getName());

        try (KafkaConsumer<String, Double> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));

            int emptyPolls = 0;
            while (emptyPolls < 5) { // Stops after 5 consecutive empty polls, adjust as needed
                ConsumerRecords<String, Double> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    emptyPolls++;
                } else {
                    emptyPolls = 0;
                    for (ConsumerRecord<String, Double> rec : records) {
                        SpeedEvent ev = new SpeedEvent(Instant.ofEpochMilli(rec.timestamp()), rec.key(), rec.value());
                        runtime.sendEvent(ev);
                    }
                }
            }
        }
        System.out.println("--- CEP Processing Finished ---");
    }
}