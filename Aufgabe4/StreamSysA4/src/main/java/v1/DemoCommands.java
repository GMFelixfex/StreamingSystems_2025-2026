package v1;

import v1.domain.Position;
import v1.write.IVehicleCommands;
import v1.write.InMemoryEventProducer;
import v1.write.VehicleCommandHandler;

// Demo class to showcase the usage of the command handler and event producer by executing a series of
// vehicle-related commands and printing the resulting events to the console.
public class DemoCommands {
    public static void main(String[] args) throws Exception {

        // Create an in-memory event producer and a vehicle command handler that uses it.
        InMemoryEventProducer producer = new InMemoryEventProducer();
        IVehicleCommands commands = new VehicleCommandHandler(producer);

        // Execute a series of commands to create, move, and remove a vehicle named "car1".
        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));
        commands.moveVehicle("car1", new Position(0, 2));
        commands.removeVehicle("car1");

        // Print all the events that were produced as a result of the commands.
        producer.getEvents().forEach(System.out::println);
    }
}