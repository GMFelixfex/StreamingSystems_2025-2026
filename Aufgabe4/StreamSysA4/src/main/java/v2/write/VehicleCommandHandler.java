package v2.write;

import v2.domain.*;

import java.util.HashSet;
import java.util.Set;

// Implementation of the IVehicleCommands interface that handles the business logic for creating, moving, and removing vehicles,
// by loading the current state of the vehicle aggregate, applying the necessary checks and rules, and
// producing the appropriate events to the event producer based on the commands received.
public class VehicleCommandHandler implements IVehicleCommands {

    private final IEventProducer eventProducer;
    private final VehicleAggregateLoader aggregateLoader;

    public VehicleCommandHandler(IEventProducer eventProducer, VehicleAggregateLoader aggregateLoader) {
        this.eventProducer = eventProducer;
        this.aggregateLoader = aggregateLoader;
    }

    // Handles the createVehicle command by checking if a vehicle with the given name already exists and is not deleted,
    // and if not, produces a VehicleCreatedEvent to the event producer.
    @Override
    public void createVehicle(String name, Position startPosition) throws Exception {
        VehicleAggregate aggregate = aggregateLoader.loadAggregate(name);

        if (aggregate.exists() && !aggregate.isDeleted()) {
            throw new IllegalArgumentException("Vehicle name '" + name + "' already exists");
        }

        VehicleCreatedEvent event = new VehicleCreatedEvent(name, startPosition);
        eventProducer.send(event);
    }

    // Handles the moveVehicle command by checking if the vehicle exists and is not deleted, validating the move vector,
    // and if valid, producing a VehicleMovedEvent to the event producer.
    @Override
    public void moveVehicle(String name, Position moveVector) throws Exception {
        VehicleAggregate aggregate = aggregateLoader.loadAggregate(name);

        if (!aggregate.exists() || aggregate.isDeleted()) {
            throw new IllegalArgumentException("Unknown vehicle: " + name);
        }

        if (moveVector.getX() == 0 && moveVector.getY() == 0) {
            throw new IllegalArgumentException("Move vector must not be zero");
        }

        VehicleMovedEvent event = new VehicleMovedEvent(name, moveVector);
        eventProducer.send(event);
    }

    // Handles the removeVehicle command by checking if the vehicle exists and is not deleted, and if so,
    // producing a VehicleRemovedEvent to the event producer.
    @Override
    public void removeVehicle(String name) throws Exception {
        VehicleAggregate aggregate = aggregateLoader.loadAggregate(name);

        if (!aggregate.exists() || aggregate.isDeleted()) {
            throw new IllegalArgumentException("Unknown vehicle: " + name);
        }

        VehicleRemovedEvent event = new VehicleRemovedEvent(name);
        eventProducer.send(event);
    }
}
