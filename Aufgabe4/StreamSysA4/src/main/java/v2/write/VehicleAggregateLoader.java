package v2.write;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import v2.domain.IVehicleEvent;
import v2.domain.VehicleAggregate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class VehicleAggregateLoader {

    // Variant 1: In-Memory Event Store
    private final IEventStoreRepository eventStoreRepository;

    // Variant 2: Kafka
    private final KafkaConsumer<String, IVehicleEvent> kafkaConsumer;
    private final String topic;

    // Constructor for in-memory event store variant, which initializes the event store repository and leaves Kafka consumer and topic uninitialized.
    public VehicleAggregateLoader(IEventStoreRepository eventStoreRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.kafkaConsumer = null;
        this.topic = null;
    }

    // Constructor for Kafka variant, which initializes the Kafka consumer and topic, and subscribes the consumer to the specified topic.
    public VehicleAggregateLoader(KafkaConsumer<String, IVehicleEvent> kafkaConsumer, String topic) {
        this.kafkaConsumer = kafkaConsumer;
        this.topic = topic;
        this.eventStoreRepository = null;
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    // Loads the VehicleAggregate for a given vehicle name by retrieving all relevant events from the configured
    // event source (either in-memory or Kafka) and applying them to reconstruct the aggregate's state.
    public VehicleAggregate loadAggregate(String name) {
        if (eventStoreRepository != null) {  // In-Memory Event Store
            List<IVehicleEvent> events = eventStoreRepository.loadEventsForVehicle(name);
            return VehicleAggregate.fromEvents(name, events);
        } else if (kafkaConsumer != null) {  // Kafka
            List<IVehicleEvent> events = new ArrayList<>();
            Set<TopicPartition> partitions = kafkaConsumer.assignment();
            if (partitions.isEmpty()) {
                kafkaConsumer.poll(Duration.ofMillis(100));
                partitions = kafkaConsumer.assignment();
            }
            kafkaConsumer.seekToBeginning(partitions);

            // Continuously poll the Kafka topic for new events until there are no more events to read, filtering for events that match the given vehicle name.
            while (true) {
                ConsumerRecords<String, IVehicleEvent> records =
                        kafkaConsumer.poll(Duration.ofMillis(200));
                if (records.isEmpty()) {
                    break;
                }
                for (ConsumerRecord<String, IVehicleEvent> record : records) {
                    IVehicleEvent event = record.value();
                    if (event != null && name.equals(event.getName())) {
                        events.add(event);
                    }
                }
            }
            return VehicleAggregate.fromEvents(name, events);
        } else {
            throw new IllegalStateException("No event source configured for VehicleAggregateLoader");
        }
    }
}
