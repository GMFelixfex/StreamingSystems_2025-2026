package v2;

import v2.domain.*;
import v2.read.IQuery;
import v2.read.InMemoryReadModel;

import java.util.List;

public class DemoQuery {
    public static void main(String[] args) {

        List<IVehicleEvent> events = List.of(
                new VehicleCreatedEvent("car1", new Position(0, 0)),
                new VehicleMovedEvent("car1", new Position(1, 0)),
                new VehicleMovedEvent("car1", new Position(0, 2))
        );

        InMemoryReadModel readModel = new InMemoryReadModel();
        IQuery query = readModel;

        for (IVehicleEvent event : events) {
            if (event instanceof VehicleCreatedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleMovedEvent e) {
                readModel.apply(e);
            } else if (event instanceof VehicleRemovedEvent e) {
                readModel.apply(e);
            }
        }

        IVehicleDTO car = query.getVehicleByName("car1");
        if (car != null) {
            System.out.println(car.getName() + " @ " + car.getPosition()
                    + " moves=" + car.getNumberOfMoves());
        } else {
            System.out.println("car1 nicht gefunden");
        }
    }
}
