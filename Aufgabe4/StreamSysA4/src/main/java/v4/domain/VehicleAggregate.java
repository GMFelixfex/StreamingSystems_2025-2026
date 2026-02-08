package v4.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Represents the aggregate state of a vehicle, which can be reconstructed from a list of events.
// It tracks whether the vehicle exists, its current position, the number of moves made, and the set of visited positions.
public class VehicleAggregate {

    private final String name;
    private boolean exists;
    private boolean deleted;
    private Position position;
    private int numberOfMoves;
    private final Set<Position> visitedPositions = new HashSet<>();

    private VehicleAggregate(String name) {
        this.name = name;
        this.exists = false;
        this.deleted = false;
        this.position = null;
        this.numberOfMoves = 0;
    }

    // Factory method to create a VehicleAggregate from a list of events.
    // It processes the events in order to reconstruct the current state of the vehicle, including tracking visited positions.
    public static VehicleAggregate fromEvents(String name, List<IVehicleEvent> events) {
        VehicleAggregate aggregate = new VehicleAggregate(name);

        for (IVehicleEvent event : events) {
            if (event instanceof VehicleCreatedEvent e) {
                aggregate.exists = true;
                aggregate.deleted = false;
                aggregate.position = e.getStartPosition();
                aggregate.numberOfMoves = 0;
                // NEW: When processing a creation event, we also initialize the set of visited positions with the starting position.
                aggregate.visitedPositions.clear();
                aggregate.visitedPositions.add(aggregate.position);
            } else if (event instanceof VehicleMovedEvent e) {
                if (!aggregate.exists || aggregate.deleted) {
                    continue;
                }
                // NEW: When processing a move event, we calculate the new position and also add it to the set of visited positions.
                Position move = e.getMoveVector();
                Position oldPos = aggregate.position;
                Position newPos = new Position(
                        oldPos.getX() + move.getX(),
                        oldPos.getY() + move.getY()
                );
                aggregate.position = newPos;
                aggregate.numberOfMoves++;
                aggregate.visitedPositions.add(newPos);
            } else if (event instanceof VehicleRemovedEvent) {
                aggregate.deleted = true;
            }
        }

        return aggregate;
    }

    // Getters for the vehicle's existence status, deletion status, current position, number of moves, and visited positions.
    public boolean exists() { return exists; }
    public boolean isDeleted() { return deleted; }
    public Position getPosition() { return position; }
    public int getNumberOfMoves() { return numberOfMoves; }
    public Set<Position> getVisitedPositions() { return Collections.unmodifiableSet(visitedPositions); }
}
