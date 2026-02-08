package v3.write;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import v3.domain.IVehicleEvent;
import v3.domain.VehicleAggregate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

// Class responsible for loading the state of a VehicleAggregate by retrieving and applying all relevant events from either an in-memory event store or a Kafka topic,
// depending on the configuration.
public class VehicleAggregateLoader {

    // Variant 1: In-Memory Event Store
    private final IEventStoreRepository eventStoreRepository;

    // Variant 2: Kafka
    private final KafkaConsumer<String, IVehicleEvent> kafkaConsumer;
    private final String topic;

    // Constructor for the in-memory event store variant, which takes an IEventStoreRepository to load events for a specific vehicle.
    public VehicleAggregateLoader(IEventStoreRepository eventStoreRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.kafkaConsumer = null;
        this.topic = null;
    }

    // Constructor for the Kafka variant, which takes a KafkaConsumer and a topic name to load events for a specific vehicle from the Kafka topic.
    public VehicleAggregateLoader(KafkaConsumer<String, IVehicleEvent> kafkaConsumer, String topic) {
        this.kafkaConsumer = kafkaConsumer;
        this.topic = topic;
        this.eventStoreRepository = null;
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    // Loads the VehicleAggregate for a given vehicle name by retrieving all relevant events from the configured
    // event source (either in-memory or Kafka) and applying them to reconstruct the aggregate's state.
    public VehicleAggregate loadAggregate(String name) {
        if (eventStoreRepository != null) {
            List<IVehicleEvent> events = eventStoreRepository.loadEventsForVehicle(name);
            return VehicleAggregate.fromEvents(name, events);
        } else if (kafkaConsumer != null) {

            List<IVehicleEvent> events = new ArrayList<>();

            // Ensure we start reading from the beginning of the topic to get all events for the vehicle, which is necessary for reconstructing the aggregate's state.
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
