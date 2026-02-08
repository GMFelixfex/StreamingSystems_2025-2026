package v3;

import org.junit.jupiter.api.Test;
import v3.domain.*;
import v3.read.InMemoryReadModel;
import v3.write.EventStoreRepository;
import v3.write.InMemoryEventProducer;
import v3.write.VehicleAggregateLoader;
import v3.write.VehicleCommandHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCommandHandlerTest {

    @Test
    void create_and_move_vehicle_produces_expected_events() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);

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
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);


        commands.createVehicle("car1", new Position(0, 0));

        assertThrows(IllegalArgumentException.class, () ->
                commands.createVehicle("car1", new Position(1, 1))
        );
    }

    @Test
    void move_with_zero_vector_is_rejected() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);


        commands.createVehicle("car1", new Position(0, 0));

        assertThrows(IllegalArgumentException.class, () ->
                commands.moveVehicle("car1", new Position(0, 0))
        );
    }

    @Test
    void fifth_move_removes_vehicle_instead_of_moving() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);

        commands.createVehicle("car1", new Position(0, 0));

        long moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        long removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(0, moveCount);
        assertEquals(0, removeCount);

        commands.moveVehicle("car1", new Position(1, 0));

        moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(1, moveCount);
        assertEquals(0, removeCount);

        commands.moveVehicle("car1", new Position(1, 0));

        moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(2, moveCount);
        assertEquals(0, removeCount);

        commands.moveVehicle("car1", new Position(1, 0));

        moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(3, moveCount);
        assertEquals(0, removeCount);

        commands.moveVehicle("car1", new Position(1, 0));

        moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(4, moveCount);
        assertEquals(0, removeCount);

        commands.moveVehicle("car1", new Position(1, 0));

        moveCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleMovedEvent)
                .count();
        removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();
        assertEquals(4, moveCount);
        assertEquals(1, removeCount);

    }

    @Test
    void move_to_already_visited_position_removes_vehicle() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);

        commands.createVehicle("car1", new Position(0, 0));
        commands.moveVehicle("car1", new Position(1, 0));  // Position (1,0)
        commands.moveVehicle("car1", new Position(1, 0));  // Position (2,0)

        commands.moveVehicle("car1", new Position(-1, 0));

        long removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();

        assertEquals(1, removeCount);
    }

    @Test
    void after_removal_name_can_be_reused() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);

        commands.createVehicle("car1", new Position(0, 0));
        commands.removeVehicle("car1");

        assertDoesNotThrow(() ->
                commands.createVehicle("car1", new Position(10, 10))
        );
    }

    @Test
    void collision() throws Exception {
        InMemoryEventProducer producer = new InMemoryEventProducer();
        EventStoreRepository repo = new EventStoreRepository(producer);
        VehicleAggregateLoader loader = new VehicleAggregateLoader(repo);
        InMemoryReadModel readModel = new InMemoryReadModel();
        VehicleCommandHandler commands = new VehicleCommandHandler(producer, loader, readModel);

        // Helper, um Events ins ReadModel zu projizieren
        Runnable replayToReadModel = () -> {
            for (IVehicleEvent event : producer.getEvents()) {
                if (event instanceof VehicleCreatedEvent e) {
                    readModel.apply(e);
                } else if (event instanceof VehicleMovedEvent e) {
                    readModel.apply(e);
                } else if (event instanceof VehicleRemovedEvent e) {
                    readModel.apply(e);
                }
            }
        };

        commands.createVehicle("car1", new Position(0, 0));
        commands.createVehicle("car2", new Position(0, 2));
        replayToReadModel.run();

        commands.moveVehicle("car1", new Position(0, 1)); // car1 @ (0,1)
        replayToReadModel.run();

        commands.moveVehicle("car1", new Position(0, 1)); // Ziel (0,2) -> car2 steht dort
        replayToReadModel.run();

        long removeCount = producer.getEvents().stream()
                .filter(e -> e instanceof VehicleRemovedEvent)
                .count();

        assertEquals(1, removeCount);
    }

}