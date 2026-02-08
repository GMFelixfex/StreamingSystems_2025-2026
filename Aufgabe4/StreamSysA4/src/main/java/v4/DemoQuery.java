package v4;

import v4.domain.*;
import v4.read.IQuery;
import v4.read.InMemoryReadModel;

import java.util.List;

// Demo class to demonstrate the usage of the read model and query interface for retrieving vehicle information based on a series of events.
public class DemoQuery {
    public static void main(String[] args) {

        // Sample events to populate the read model for demonstration purposes.
        List<IVehicleEvent> events = List.of(
                new VehicleCreatedEvent("car1", new Position(0, 0)),
                new VehicleMovedEvent("car1", new Position(1, 0)),
                new VehicleMovedEvent("car1", new Position(0, 2))
        );
        
        // Create an instance of the InMemoryReadModel and use it as the implementation of the IQuery interface.
        InMemoryReadModel readModel = new InMemoryReadModel();
        IQuery query = readModel;

        // Apply the events to the read model to build up the current state of the vehicles.
        for (IVehicleEvent event : events) {
            if (event instanceof VehicleCreatedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleMovedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleRemovedEvent e) {
                readModel.apply(e);
            }
        }

        // Use the query interface to retrieve information about a specific vehicle and print it out.
        IVehicleDTO car = query.getVehicleByName("car1");
        if (car != null) {
            System.out.println(car.getName() + " @ " + car.getPosition()
                    + " moves=" + car.getNumberOfMoves());
        } else {
            System.out.println("car1 nicht gefunden");
        }
    }
}
