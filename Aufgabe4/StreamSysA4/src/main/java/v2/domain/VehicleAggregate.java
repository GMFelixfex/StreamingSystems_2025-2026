package v2.domain;

import java.util.List;

// Aggregate representing the current state of a vehicle, reconstructed from a list of events related to that vehicle.
public class VehicleAggregate {

    private final String name;
    private boolean exists;
    private boolean deleted;
    private Position position;
    private int numberOfMoves;

    private VehicleAggregate(String name) {
        this.name = name;
        this.exists = false;
        this.deleted = false;
        this.position = null;
        this.numberOfMoves = 0;
    }

    // Factory method to create a VehicleAggregate from a list of events. It processes the events in order to reconstruct the current state of the vehicle.
    public static VehicleAggregate fromEvents(String name, List<IVehicleEvent> events) {
        VehicleAggregate aggregate = new VehicleAggregate(name);

        for (IVehicleEvent event : events) {
            if (event instanceof VehicleCreatedEvent e) {
                aggregate.exists = true;
                aggregate.deleted = false;
                aggregate.position = e.getStartPosition();
                aggregate.numberOfMoves = 0;
            } else if (event instanceof VehicleMovedEvent e) {
                if (!aggregate.exists || aggregate.deleted) {
                    continue;
                }
                Position move = e.getMoveVector();
                Position oldPos = aggregate.position;
                aggregate.position = new Position(
                        oldPos.getX() + move.getX(),
                        oldPos.getY() + move.getY()
                );
                aggregate.numberOfMoves++;
            } else if (event instanceof VehicleRemovedEvent) {
                aggregate.deleted = true;
            }
        }

        return aggregate;
    }

    // Getters for the vehicle's existence status, deletion status, current position, and number of moves.
    public boolean exists() {return exists;}
    public boolean isDeleted() {return deleted;}
    public Position getPosition() {return position;}
    public int getNumberOfMoves() {return numberOfMoves;}
}
