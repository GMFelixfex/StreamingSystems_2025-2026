package v1.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import v1.domain.IVehicleEvent;

import java.util.Properties;

// Factory class for creating Kafka consumers that can consume IVehicleEvent messages from a Kafka topic.
public class KafkaEventConsumerFactory {

    public static KafkaConsumer<String, IVehicleEvent> createConsumer(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", "v1.serialization.VehicleEventDeserializer");
        props.put("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(props);
    }
}
