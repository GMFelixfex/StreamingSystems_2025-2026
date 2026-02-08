package v1;

import v1.domain.*;
import v1.read.IQuery;
import v1.read.InMemoryReadModel;

import java.util.List;

// Demo class to showcase the usage of the read model and query interface by applying a series of vehicle-related
// events and querying the resulting state of a vehicle.
public class DemoQuery {
    public static void main(String[] args) {
        // Create a list of vehicle events that represent the creation and movement of a vehicle named "car1".
        List<IVehicleEvent> events = List.of(
                new VehicleCreatedEvent("car1", new Position(0, 0)),
                new VehicleMovedEvent("car1", new Position(1, 0)),
                new VehicleMovedEvent("car1", new Position(0, 2))
        );

        // Create an in-memory read model and a query interface that uses it.
        InMemoryReadModel readModel = new InMemoryReadModel();
        IQuery query = readModel;

        // Apply the events to the read model to update its state based on the vehicle's creation and movements.
        for (IVehicleEvent event : events) {
            if (event instanceof VehicleCreatedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleMovedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleRemovedEvent e) {
                readModel.apply(e);
            }
        }

        // Query the read model for the current state of "car1" and print its name, position, and number of moves.
        IVehicleDTO car = query.getVehicleByName("car1");
        if (car != null) {
            System.out.println(car.getName() + " @ " + car.getPosition()
                    + " moves=" + car.getNumberOfMoves());
        } else {
            System.out.println("car1 nicht gefunden");
        }
    }
}
