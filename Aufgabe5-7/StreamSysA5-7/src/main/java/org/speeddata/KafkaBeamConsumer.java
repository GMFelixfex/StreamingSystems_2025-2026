package org.speeddata;


import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.joda.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class KafkaBeamConsumer {

    // Use SLF4J Logger (which now routes to Log4j2)
    private static final Logger LOG = LoggerFactory.getLogger(KafkaBeamConsumer.class);

    public static void main(String[] args) {
        PipelineOptions options = PipelineOptionsFactory.fromArgs(args).create();
        options.setRunner(DirectRunner.class);

        Pipeline pipeline = Pipeline.create(options);

        // Use a unique group ID to force a fresh connection and metadata update
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put("group.id", "beam-log4j-group-" + System.currentTimeMillis());
        consumerConfig.put("auto.offset.reset", "earliest");

        pipeline
                .apply("ReadFromKafka", KafkaIO.<String, String>read()
                        .withBootstrapServers("localhost:9092")
                        .withTopic("speeddata")
                        .withKeyDeserializer(StringDeserializer.class)
                        .withValueDeserializer(StringDeserializer.class)
                        .withConsumerConfigUpdates(consumerConfig)
                        .withStartReadTime(Instant.EPOCH)
                        .withoutMetadata()
                )
                .apply("ExtractPayload", Values.create())
                .apply("LogData", ParDo.of(new DoFn<String, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        // This log should appear in your console now
                        LOG.info(">>> MESSAGE RECEIVED: {}", c.element());
                        System.out.println(">>> MESSAGE RECEIVED: " + c.element());
                    }
                }));

        LOG.info(">>> Starting Pipeline execution...");
        System.out.println(">>> Starting Pipeline execution...");
        pipeline.run().waitUntilFinish();
    }
}