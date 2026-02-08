package v3;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import v3.domain.IVehicleDTO;
import v3.domain.IVehicleEvent;
import v3.domain.Position;
import v3.kafka.KafkaEventConsumerFactory;
import v3.read.InMemoryReadModel;
import v3.read.VehicleProjection;
import v3.write.EventProducer;
import v3.write.IVehicleCommands;
import v3.write.VehicleAggregateLoader;
import v3.write.VehicleCommandHandler;

// Main class demonstrating the usage of the Kafka-based event sourcing system for managing vehicle commands and projections.
public class KafkaDemo {

    public static void main(String[] args) throws Exception {
        String bootstrap = "localhost:9092";
        String topic = "vehicle-events";

        // Write Side: EventProducer + CommandHandler (Kafka-Producer)
        EventProducer kafkaProducer = new EventProducer(bootstrap, topic);

        KafkaConsumer<String, IVehicleEvent> aggregateConsumer =
                KafkaEventConsumerFactory.createConsumer(
                        bootstrap,
                        "vehicle-aggregate-v3-" + System.currentTimeMillis()
                );

        VehicleAggregateLoader aggregateLoader =
                new VehicleAggregateLoader(aggregateConsumer, topic);

        // Read Side: Projection (Kafka-Consumer) + InMemoryReadModel
        InMemoryReadModel readModel = new InMemoryReadModel();
        KafkaConsumer<String, IVehicleEvent> projectionConsumer =
                KafkaEventConsumerFactory.createConsumer(
                        bootstrap,
                        "vehicle-projection-v3-" + System.currentTimeMillis()
                );
        VehicleProjection projection = new VehicleProjection(projectionConsumer, readModel);
        Thread projectionThread = new Thread(projection, "vehicle-projection");
        projectionThread.setDaemon(true);
        projectionThread.start();

        // Command Handler that uses the EventProducer to send commands and the AggregateLoader to load the current state of the vehicle aggregates.
        IVehicleCommands commands =
                new VehicleCommandHandler(kafkaProducer, aggregateLoader, readModel);

        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(0, 5));
        commands.moveVehicle("car1", new Position(10, 0));
        commands.moveVehicle("car1", new Position(1, 1));
        commands.moveVehicle("car1", new Position(0, -1));
        commands.moveVehicle("car1", new Position(-1, 0));

        commands.createVehicle("car2", new Position(0, 0));
        commands.createVehicle("car3", new Position(0, 2));
        commands.moveVehicle("car2", new Position(0, 1));
        commands.moveVehicle("car2", new Position(0, 1));


        Thread.sleep(3000);

        System.out.println("Vehicles in ReadModel: "
                + java.util.Collections.list(readModel.getVehicles()).size());

        int tries = 0;
        IVehicleDTO car = readModel.getVehicleByName("car1");
        while (tries < 20 && car == null) {
            Thread.sleep(100);
            car = readModel.getVehicleByName("car1");
            tries++;
        }

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
