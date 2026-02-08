package v1.write;

import org.junit.jupiter.api.Test;
import v1.domain.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCommandHandlerTest {

    @Test
    void create_and_move_vehicle_produces_expected_events() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer);

        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));
        commands.moveVehicle("car1", new Position(0, 2));
        commands.removeVehicle("car1");

        List<IVehicleEvent> events = producer.getEvents();
        assertEquals(4, events.size());

        assertInstanceOf(VehicleCreatedEvent.class, events.get(0));
        assertInstanceOf(VehicleMovedEvent.class, events.get(1));
        assertInstanceOf(VehicleMovedEvent.class, events.get(2));
        assertInstanceOf(VehicleRemovedEvent.class, events.get(3));

        VehicleCreatedEvent created = (VehicleCreatedEvent) events.get(0);
        assertEquals("car1", created.getName());
        assertEquals(0, created.getStartPosition().getX());
        assertEquals(0, created.getStartPosition().getY());
    }

    @Test
    void cannot_create_vehicle_with_duplicate_name() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer);

        commands.createVehicle("car1", new Position(0, 0));

        assertThrows(IllegalArgumentException.class, () ->
                commands.createVehicle("car1", new Position(1, 1))
        );
    }

    @Test
    void move_with_zero_vector_is_rejected() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer);

        commands.createVehicle("car1", new Position(0, 0));

        assertThrows(IllegalArgumentException.class, () ->
                commands.moveVehicle("car1", new Position(0, 0))
        );
    }
}