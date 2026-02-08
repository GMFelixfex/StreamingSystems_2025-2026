package v4.write;

import v4.domain.*;
import v4.read.IQuery;

import java.util.Enumeration;

// Implementation of the IVehicleCommands interface that handles the business logic for creating, moving, and removing vehicles,
// by loading the current state of the vehicle aggregate, applying the necessary checks and rules, and
// producing the appropriate events to the event producer based on the commands received.
public class VehicleCommandHandler implements IVehicleCommands {

    private final IEventProducer eventProducer;
    private final VehicleAggregateLoader aggregateLoader;
    private final IQuery query;

    public VehicleCommandHandler(IEventProducer eventProducer, VehicleAggregateLoader aggregateLoader, IQuery query) {
        this.eventProducer = eventProducer;
        this.aggregateLoader = aggregateLoader;
        this.query = query;

    }

    // Handles the createVehicle command by loading the vehicle aggregate. If the vehicle already exists and is not deleted, it throws an exception.
    // Otherwise, it creates a VehicleCreatedEvent and sends it to the event producer.
    @Override
    public void createVehicle(String name, Position startPosition) throws Exception {
        VehicleAggregate aggregate = aggregateLoader.loadAggregate(name);

        if (aggregate.exists() && !aggregate.isDeleted()) {
            throw new IllegalArgumentException("Vehicle name '" + name + "' already exists");
        }

        VehicleCreatedEvent event = new VehicleCreatedEvent(name, startPosition);
        eventProducer.send(event);
    }

    // Handles the moveVehicle command by loading the vehicle aggregate and performing several checks:
    // - If the vehicle does not exist or is deleted, it throws an exception.
    // - If the move vector is zero, it throws an exception.
    // - If the vehicle has already moved 4 times, it produces a VehicleRemovedEvent.
    // - If the target position has been visited before, it produces a VehicleRemovedEvent.
    // - If there are other vehicles at the target position, it produces VehicleRemovedEvents for those vehicles.
    // If all checks pass, it produces a VehicleMovedEvent to move the vehicle to the target position.
    @Override
    public void moveVehicle(String name, Position moveVector) throws Exception {
        VehicleAggregate aggregate = aggregateLoader.loadAggregate(name);

        // Check if the vehicle exists and is not deleted.
        if (!aggregate.exists() || aggregate.isDeleted()) {
            throw new IllegalArgumentException("Unknown vehicle: " + name);
        }

        // Check if the move vector is zero.
        if (moveVector.getX() == 0 && moveVector.getY() == 0) {
            throw new IllegalArgumentException("Move vector must not be zero");
        }

        // Check if the vehicle has already moved 5 times.
        if (aggregate.getNumberOfMoves() >= 4) {
            eventProducer.send(new VehicleRemovedEvent(name));
            return;
        }

        Position currentPos = aggregate.getPosition();
        Position targetPos = new Position(
                currentPos.getX() + moveVector.getX(),
                currentPos.getY() + moveVector.getY()
        );
        
        // Check if the target position has been visited before.
        if (aggregate.getVisitedPositions().contains(targetPos)) {
            eventProducer.send(new VehicleRemovedEvent(name));
            return;
        }

        // NEW: Check if there are other vehicles at the target position and remove them if necessary.
        Enumeration<IVehicleDTO> vehiclesAtTarget = query.getVehiclesAtPosition(targetPos);
        while (vehiclesAtTarget.hasMoreElements()) {
            IVehicleDTO other = vehiclesAtTarget.nextElement();
            if (!other.getName().equals(name)) {
                eventProducer.send(new VehicleRemovedEvent(other.getName()));
            }
        }

        VehicleMovedEvent event = new VehicleMovedEvent(name, moveVector);
        eventProducer.send(event);
    }

    // Handles the removeVehicle command by loading the vehicle aggregate. If the vehicle does not exist or is already deleted, it throws an exception.
    // Otherwise, it creates a VehicleRemovedEvent and sends it to the event producer.
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
