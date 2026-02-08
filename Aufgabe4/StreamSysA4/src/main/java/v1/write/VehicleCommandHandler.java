package v1.write;

import v1.domain.Position;
import v1.domain.VehicleCreatedEvent;
import v1.domain.VehicleMovedEvent;
import v1.domain.VehicleRemovedEvent;

import java.util.Set;
import java.util.HashSet;

// Implementation of the IVehicleCommands interface that handles vehicle-related commands (create, move, remove)
// and produces corresponding events using an IEventProducer. It also maintains a set of existing vehicle names to enforce uniqueness and validate commands.
public class VehicleCommandHandler implements IVehicleCommands {

    private final Set<String> vehicleNames = new HashSet<>();
    private final IEventProducer eventProducer;

    public VehicleCommandHandler(IEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    // Handles the creation of a new vehicle by validating the name, creating a VehicleCreatedEvent, and sending it through the event producer.
    @Override
    public void createVehicle(String name, Position startPosition) throws Exception {
        if (vehicleNames.contains(name)) {
            throw new IllegalArgumentException("Vehicle name '" + name + "' already exists");
        }
        VehicleCreatedEvent event = new VehicleCreatedEvent(name, startPosition);
        vehicleNames.add(name);
        eventProducer.send(event);
    }

    // Handles the movement of an existing vehicle by validating the name and move vector, creating a VehicleMovedEvent, and sending it through the event producer.
    @Override
    public void moveVehicle(String name, Position moveVector) throws Exception {
        if (!vehicleNames.contains(name)) {
            throw new IllegalArgumentException("Unknown vehicle: " + name);
        }
        if (moveVector.getX() == 0 && moveVector.getY() == 0) {
            throw new IllegalArgumentException("Move vector must not be zero");
        }
        VehicleMovedEvent event = new VehicleMovedEvent(name, moveVector);
        eventProducer.send(event);

    }

    // Handles the removal of an existing vehicle by validating the name, creating a VehicleRemovedEvent, and sending it through the event producer.
    // It also removes the vehicle name from the set of existing names.
    @Override
    public void removeVehicle(String name) throws Exception {
        if (!vehicleNames.contains(name)) {
            throw new IllegalArgumentException("Unknown vehicle: " + name);
        }
        VehicleRemovedEvent event = new VehicleRemovedEvent(name);
        vehicleNames.remove(name);
        eventProducer.send(event);

    }
}
