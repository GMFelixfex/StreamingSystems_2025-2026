package v2;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import v2.domain.IVehicleDTO;
import v2.domain.IVehicleEvent;
import v2.domain.Position;
import v2.kafka.KafkaEventConsumerFactory;
import v2.read.InMemoryReadModel;
import v2.read.VehicleProjection;
import v2.write.EventProducer;
import v2.write.IVehicleCommands;
import v2.write.VehicleAggregateLoader;
import v2.write.VehicleCommandHandler;

// Main class that demonstrates the integration of Kafka for event sourcing and projections in a vehicle management system.
// It sets up a Kafka producer to send commands, a consumer to load aggregates, and another consumer to update the read model through a projection.
// Finally, it executes some commands and queries the read model to verify the results.
public class KafkaDemo {

    public static void main(String[] args) throws Exception {
        String bootstrap = "localhost:9092";
        String topic = "vehicle-events";

        // Create a Kafka event producer to send commands as events to the specified topic.
        EventProducer kafkaProducer = new EventProducer(bootstrap, topic);

        KafkaConsumer<String, IVehicleEvent> aggregateConsumer =
                KafkaEventConsumerFactory.createConsumer(
                        bootstrap,
                        "vehicle-aggregate-v2-" + System.currentTimeMillis()
                );

        // Create an aggregate loader that uses the Kafka consumer to load the state of vehicle aggregates from the events in the topic.
        VehicleAggregateLoader aggregateLoader = new VehicleAggregateLoader(aggregateConsumer, topic);

        // Create the command handler that will handle vehicle commands and produce events using the Kafka producer, while also loading aggregate state using the aggregate loader.
        IVehicleCommands commands = new VehicleCommandHandler(kafkaProducer, aggregateLoader);

        // Set up the read model and projection to consume events from the Kafka topic and update the in-memory read model accordingly.
        InMemoryReadModel readModel = new InMemoryReadModel();
        KafkaConsumer<String, IVehicleEvent> projectionConsumer =
                KafkaEventConsumerFactory.createConsumer(
                        bootstrap,
                        "vehicle-projection-v2-" + System.currentTimeMillis()
                );

        // Create and start the projection in a separate thread to continuously consume events and update the read model.
        VehicleProjection projection = new VehicleProjection(projectionConsumer, readModel);
        Thread projectionThread = new Thread(projection, "vehicle-projection");
        projectionThread.setDaemon(true);
        projectionThread.start();

        // Execute a series of commands to create and move a vehicle, which will produce events that are consumed by the projection to update the read model.
        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));
        commands.moveVehicle("car1", new Position(0, 2));

        // Wait a bit to allow the projection to consume the events and update the read model before querying it.
        Thread.sleep(3000);

        System.out.println("Vehicles in ReadModel: "
                + java.util.Collections.list(readModel.getVehicles()).size());

        // Query the read model for the current state of "car1" and print its name, position, and number of moves.
        int tries = 0;
        IVehicleDTO car = readModel.getVehicleByName("car1");
        // In case the projection hasn't processed the events yet, we can wait a bit and retry a few times before giving up.
        while (tries < 20 && car == null) {
            Thread.sleep(100);
            car = readModel.getVehicleByName("car1");
            tries++;
        }

        // Print the results of the query, including the number of vehicles in the read model and the details of "car1".
        System.out.println("Vehicles in ReadModel: "
                + java.util.Collections.list(readModel.getVehicles()).size());
        System.out.println("ReadModel result: " + car);
        if (car == null) {
            System.out.println("car1 not found in ReadModel");
        } else {
            System.out.println("car1 via Kafka: " + car.getName()
                    + " @ " + car.getPosition()
                    + " moves=" + car.getNumberOfMoves());
        }

        kafkaProducer.close();
        aggregateConsumer.close();
    }

}
