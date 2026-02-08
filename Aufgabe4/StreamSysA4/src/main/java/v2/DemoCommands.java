package v2;

import v2.domain.Position;
import v2.write.*;

// Demo class to execute a series of commands on the vehicle command handler and print the resulting events from the in-memory event producer.
public class DemoCommands {
    public static void main(String[] args) throws Exception {
        // Create an in-memory event producer to capture the events produced by the command handler.
        InMemoryEventProducer producer = new InMemoryEventProducer();

        // Set up the event store and aggregate loader, which are required by the command handler to manage the state of vehicle aggregates.
        EventStoreRepository eventStore = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(eventStore);

        // Create the command handler with the event producer and aggregate loader, which will handle the commands and produce events accordingly.
        IVehicleCommands commands = new VehicleCommandHandler(producer, loader);

        // Execute a series of commands: create a vehicle, move it twice, and then remove it. Each command will produce corresponding events that are stored in the in-memory event producer.
        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));
        commands.moveVehicle("car1", new Position(0, 2));
        commands.removeVehicle("car1");

        // Print all the events that were produced as a result of executing the commands, demonstrating the sequence of events that occurred.
        producer.getEvents().forEach(System.out::println);
    }
}
