package v1.read;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import v1.domain.IVehicleEvent;
import v1.domain.VehicleCreatedEvent;
import v1.domain.VehicleMovedEvent;
import v1.domain.VehicleRemovedEvent;

import java.time.Duration;
import java.util.Collections;

// Projection class that consumes vehicle events from a Kafka topic and updates an in-memory read model accordingly.
public class VehicleProjection implements Runnable {

    private final KafkaConsumer<String, IVehicleEvent> consumer;
    private final InMemoryReadModel readModel;

    public VehicleProjection(KafkaConsumer<String, IVehicleEvent> consumer, InMemoryReadModel readModel) {
        this.consumer = consumer;
        this.readModel = readModel;
    }

    // Main loop of the projection that continuously polls for new events and applies them to the read model.
    @Override
    public void run() {
        consumer.subscribe(Collections.singletonList("vehicle-events"));
        System.out.println("Projection started, subscribed to vehicle-events");

        while (true) {
            var records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, IVehicleEvent> record : records) {
                IVehicleEvent event = record.value();
                System.out.println("Projection received: " + event);

                // Apply the event to the read model based on its type.
                if (event instanceof VehicleCreatedEvent e) {
                    readModel.apply(e);
                } else if (event instanceof VehicleMovedEvent e) {
                    readModel.apply(e);
                } else if (event instanceof VehicleRemovedEvent e) {
                    readModel.apply(e);
                }
            }
        }
    }
}
