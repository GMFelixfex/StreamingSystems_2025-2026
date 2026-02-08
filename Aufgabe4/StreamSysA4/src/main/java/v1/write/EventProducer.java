package v1.write;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import v1.domain.IVehicleEvent;

import java.util.Properties;

// Implementation of the IEventProducer interface that sends IVehicleEvent messages to a Kafka topic using a KafkaProducer.
public class EventProducer implements IEventProducer {
    private final KafkaProducer<String, IVehicleEvent> producer;
    private final String topic;

    public EventProducer(String bootstrapServers, String topic) {
        this.topic = topic;
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "v1.serialization.VehicleEventSerializer");
        props.put("acks", "1");
        this.producer = new KafkaProducer<>(props);
    }

    // Sends an IVehicleEvent to the configured Kafka topic, using the event's name as the key for partitioning.
    @Override
    public void send(IVehicleEvent event) {
        System.out.println("Sending event to Kafka: " + event);
        ProducerRecord<String, IVehicleEvent> record =
                new ProducerRecord<>(topic, event.getName(), event);
        producer.send(record); // async reicht hier
    }

    // Flushes and closes the Kafka producer to ensure all messages are sent before shutting down.
    public void close() {
        producer.flush();
        producer.close();
    }
}
