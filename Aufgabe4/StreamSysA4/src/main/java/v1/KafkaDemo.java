package v1;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import v1.domain.IVehicleDTO;
import v1.domain.IVehicleEvent;
import v1.domain.Position;
import v1.kafka.KafkaEventConsumerFactory;
import v1.read.InMemoryReadModel;
import v1.read.VehicleProjection;
import v1.write.EventProducer;
import v1.write.IVehicleCommands;
import v1.write.VehicleCommandHandler;

import java.util.List;
import java.util.Properties;
import java.util.Set;

// Main class that demonstrates the end-to-end flow of a Kafka-based event-driven architecture for vehicle events, including producing events,
// consuming them in a projection, and querying the resulting read model.
public class KafkaDemo {

    public static void main(String[] args) throws Exception {

        // 1. Write Side: Command Handler + EventProducer (Kafka-Producer)
        EventProducer kafkaProducer = new EventProducer("localhost:9092", "vehicle-events");
        IVehicleCommands commands = new VehicleCommandHandler(kafkaProducer);

        // 2. Read Side: ReadModel + Projection (Kafka-Consumer)
        InMemoryReadModel readModel = new InMemoryReadModel();
        KafkaConsumer<String, IVehicleEvent> consumer =
                KafkaEventConsumerFactory.createConsumer(
                        "localhost:9092",
                        "vehicle-projection-v1-" + System.currentTimeMillis()
                );
        VehicleProjection projection = new VehicleProjection(consumer, readModel);
        Thread projectionThread = new Thread(projection, "vehicle-projection");
        projectionThread.setDaemon(true);
        projectionThread.start();

        // 3. Execute some commands to create and move a vehicle, which will produce events that the projection will consume to update the read model.
        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));
        commands.moveVehicle("car1", new Position(0, 2));

        // Wait a bit to allow the projection to consume the events and update the read model before querying it.
        Thread.sleep(3000);

        System.out.println("Vehicles in ReadModel: " + java.util.Collections.list(readModel.getVehicles()).size());

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
        System.out.println("Vehicles in ReadModel: " + java.util.Collections.list(readModel.getVehicles()).size());
        System.out.println("ReadModel result: " + car);
        if (car == null) {
            System.out.println("car1 not found in ReadModel");
        } else {
            System.out.println("car1 via Kafka: " + car.getName()
                    + " @ " + car.getPosition()
                    + " moves=" + car.getNumberOfMoves());
        }

        kafkaProducer.close();
    }
}
