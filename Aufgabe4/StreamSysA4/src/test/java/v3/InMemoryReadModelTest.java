package v3;

import org.junit.jupiter.api.Test;
import v1.domain.*;
import v1.read.InMemoryReadModel;

import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryReadModelTest {

    @Test
    void apply_events_updates_position_and_moves_correctly() {
        InMemoryReadModel readModel = new InMemoryReadModel();

        readModel.apply(new VehicleCreatedEvent("car1", new Position(0, 0)));
        readModel.apply(new VehicleMovedEvent("car1", new Position(1, 0)));
        readModel.apply(new VehicleMovedEvent("car1", new Position(0, 2)));

        IVehicleDTO car = readModel.getVehicleByName("car1");
        assertNotNull(car);
        assertEquals("car1", car.getName());
        assertEquals(1, car.getPosition().getX());
        assertEquals(2, car.getPosition().getY());
        assertEquals(2, car.getNumberOfMoves());
    }

    @Test
    void removed_vehicle_is_not_returned_anymore() {
        InMemoryReadModel readModel = new InMemoryReadModel();

        readModel.apply(new VehicleCreatedEvent("car1", new Position(0, 0)));
        readModel.apply(new VehicleRemovedEvent("car1"));

        IVehicleDTO car = readModel.getVehicleByName("car1");
        assertNull(car);
    }

    @Test
    void get_vehicles_at_position_returns_correct_vehicles() {
        InMemoryReadModel readModel = new InMemoryReadModel();

        readModel.apply(new VehicleCreatedEvent("car1", new Position(0, 0)));
        readModel.apply(new VehicleCreatedEvent("car2", new Position(0, 0)));
        readModel.apply(new VehicleCreatedEvent("car3", new Position(1, 0)));

        Enumeration<IVehicleDTO> atOrigin =
                readModel.getVehiclesAtPosition(new Position(0, 0));

        int count = 0;
        while (atOrigin.hasMoreElements()) {
            String name = atOrigin.nextElement().getName();
            assertTrue(name.equals("car1") || name.equals("car2"));
            count++;
        }
        assertEquals(2, count);
    }


}
