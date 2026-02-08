package org.speeddata;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

import org.apache.beam.sdk.io.kafka.TimestampPolicyFactory;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.beam.sdk.io.kafka.KafkaRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import org.apache.beam.sdk.options.StreamingOptions;

import java.util.HashMap;
import java.util.Map;

public class KafkaBeamPipeline {

    // Example input format: speeddata: key=2, value=10.3, timestamp=1731514017024
    // key = sensor id, value = speed as string, timestamp = event time from Sensor (uses Kafka record timestamp)
    // Goal get the average speed per sensor in km/h in 10-second fixed windows
    // Steps:
    // 1. Parse the value string to double speed in m/s, filter out negative records (e.g everything negative)
    // 2. Convert speed to km/h
    // 3. Window into 10-second fixed windows based on event time (Kafka record timestamp)
    // 4. Group by sensor id and window, compute average speed per sensor per window
    // 5. Format the output as a string with window start/end and average speed, write to text files with outputPrefix
    // Example output line: [18:13:38.013, 18:13:48.013] SensorId=1 AverageSpeed=26.28

    private static final String OUTPUT_FILE = "data/averages.txt";
    private static long counter = 0L;
    public static void run(String bootstrapServers, String topic) {
        StreamingOptions options = PipelineOptionsFactory.as(StreamingOptions.class);
        Pipeline p = Pipeline.create(options);

        // Read Kafka records (key=String, value=String) including Kafka timestamps
        Map<String,Object> consumerProps = new HashMap<>();
        consumerProps.put("auto.offset.reset", "earliest");
        // Use a unique consumer group per run to ensure offset reset behavior applies
        String groupId = "speeddata-beam-consumer-" + System.currentTimeMillis();
        consumerProps.put("group.id", groupId);

        System.out.println("Creating Kafka read with group.id=" + groupId + " bootstrap=" + bootstrapServers + " topic=" + topic);
        PCollection<KafkaRecord<String, Double>> collection = p.apply("ReadFromKafka", KafkaIO.<String, Double>read()
            .withBootstrapServers(bootstrapServers)
            .withTopic(topic)
            .withKeyDeserializer(org.apache.kafka.common.serialization.StringDeserializer.class)
            .withValueDeserializer(org.apache.kafka.common.serialization.DoubleDeserializer.class)
            .updateConsumerProperties(consumerProps)
            .withTimestampPolicyFactory(TimestampPolicyFactory.withCreateTime(Duration.standardSeconds(1)))
            .withReadCommitted()
);
        //log collection output for correct timestamp
        collection.apply("LogKafkaRecords", ParDo.of(new DoFn<KafkaRecord<String, Double>, KafkaRecord<String, Double>>() {
            private static final long LOG_EVERY = 100L;

            @ProcessElement
            public void processElement(ProcessContext c) {
                counter = counter + 1L;
                if ((counter % LOG_EVERY) == 0) {
                    KafkaRecord<String, Double> rec = c.element();
                    System.out.println(counter + ": latest record: key=" + rec.getKV().getKey() + ", value=" + rec.getKV().getValue() + ", timestamp=" + rec.getTimestamp());
                }
                c.outputWithTimestamp(c.element(), c.timestamp());
            }
        }));

        collection
                .apply("ExtractKeyValue", ParDo.of(new DoFn<KafkaRecord<String, Double>, KV<String, Double>>() {
                    @Override
                    public Duration getAllowedTimestampSkew() {
                        // allow outputs with timestamps older than the input by a large skew
                        return Duration.standardDays(3650); // ~10 years
                    }

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KafkaRecord<String, Double> rec = c.element();
                        KV<String, Double> kv = KV.of(rec.getKV().getKey(), rec.getKV().getValue());
                        org.joda.time.Instant ts = new org.joda.time.Instant(rec.getTimestamp());
                        c.outputWithTimestamp(kv, ts);
                    }
                }))
                .apply("FilterNegativeSpeeds", ParDo.of(new DoFn<KV<String, Double>, KV<String, Double>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, Double> kv = c.element();
                        if (kv.getValue() >= 0) {
                            c.output(kv);
                        }
                        else {
                            //System.out.println("Filtering out negative speed: " + kv.getValue() + " for sensor " + kv.getKey() + " at timestamp " + c.timestamp());
                        }
                    }
                }))
                .apply("ConvertToKmh", ParDo.of(new DoFn<KV<String, Double>, KV<String, Double>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, Double> kv = c.element();
                        KV<String, Double> out = KV.of(kv.getKey(), kv.getValue() * 3.6);
                        c.outputWithTimestamp(out, c.timestamp());
                    }
                }))

                // debug before windowing, to verify timestamps
                .apply("DebugBeforeWindow", ParDo.of(new DoFn<KV<String, Double>, KV<String, Double>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, Double> kv = c.element();
                        //System.out.println("Before windowing: key=" + kv.getKey() + ", value=" + kv.getValue() + ", timestamp=" + c.timestamp());
                        c.outputWithTimestamp(kv, c.timestamp());
                    }
                }))
                // (Fixed Window, 10s long, allowed lateness 10 years, accumulating fired panes):
                .apply("WindowInto10s", Window.<KV<String, Double>>into(FixedWindows.of(Duration.standardSeconds(10)))
                    .accumulatingFiredPanes())
                // (Sliding Window, 10s long, every 5s):
                /*.apply("WindowIntoSliding", Window.<KV<String, Double>>into(
                        SlidingWindows.of(Duration.standardSeconds(10))
                                .every(Duration.standardSeconds(5))
                ))*/
                // group by sensor id and window
                .apply("GroupBySensorId", GroupByKey.<String, Double>create())
                // compute average speed per sensor per window
                .apply("AveragePerSensorPerWindow", ParDo.of(new DoFn<KV<String, Iterable<Double>>, KV<String, Double>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, Iterable<Double>> kv = c.element();
                        String sensorId = kv.getKey();
                        Iterable<Double> speeds = kv.getValue();
                        double sum = 0.0;
                        int count = 0;
                        for (Double speed : speeds) {
                            sum += speed;
                            count++;
                        }
                        if (count > 0) {
                            double avg = sum / count;
                            c.output(KV.of(sensorId, avg));
                        }
                    }
                }))
                .apply("FormateOutputWithTimestamp", ParDo.of(new DoFn<KV<String, Double>, String>() {
                    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

                    @ProcessElement
                    public void processElement(ProcessContext c, BoundedWindow window) {
                        KV<String, Double> kv = c.element();
                        String sensorId = kv.getKey();
                        Double avgSpeed = kv.getValue();
                        IntervalWindow w = (IntervalWindow) window;
                        String startStr = TIME_FORMATTER.format(java.time.Instant.ofEpochMilli(w.start().getMillis()));
                        String endStr = TIME_FORMATTER.format(java.time.Instant.ofEpochMilli(w.end().getMillis()));
                        String output = String.format("[%s, %s] SensorId=%s AverageSpeed=%.2f",
                            startStr,
                            endStr,
                            sensorId, avgSpeed);
                        c.output(output);
                    }
                }))
                .apply("WriteToLocalFiles", ParDo.of(new SaveToFile(OUTPUT_FILE)));

        p.run();




    }
    static class SaveToFile extends DoFn<String, String> {
        private final String outputFile;

        public SaveToFile(String outputFile) {
            this.outputFile = outputFile;
        }

        @ProcessElement
        public void processElement(ProcessContext c, BoundedWindow window) {
            String line = c.element();
            IntervalWindow w = (IntervalWindow) window;
            // write all windows into a single file (one line per emitted element)
            try {
                Path p = Paths.get(outputFile);
                Path parent = p.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.write(p, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ex) {
                throw new RuntimeException("Failed writing output file: " + outputFile, ex);
            }
            // pass through (no further consumers in this pipeline)
            c.output(line);
        }
    }

    
}
