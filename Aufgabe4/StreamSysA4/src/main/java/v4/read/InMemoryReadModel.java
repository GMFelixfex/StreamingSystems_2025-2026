package v4.read;

import v4.domain.*;

import java.util.*;

// In-memory implementation of the read model that maintains the current state of vehicles based on the events it has processed.
public class InMemoryReadModel implements IQuery {
    private final Map<String, VehicleInfo> vehiclesByName = new HashMap<>();
    private final Map<Position, Set<String>> vehiclesByPosition = new HashMap<>();

    // Returns the current state of a vehicle by its name, or null if no such vehicle exists.
    @Override
    public IVehicleDTO getVehicleByName(String name) {
        return vehiclesByName.get(name);
    }

    // Returns an enumeration of all vehicles currently in the system.
    @Override
    public Enumeration<IVehicleDTO> getVehicles() {
        return Collections.enumeration(new ArrayList<>(vehiclesByName.values()));
    }

    // Returns an enumeration of all vehicles currently at the specified position.
    @Override
    public Enumeration<IVehicleDTO> getVehiclesAtPosition(Position position) {
        Set<String> names = vehiclesByPosition.getOrDefault(position, Collections.emptySet());
        List<IVehicleDTO> result = new ArrayList<>();
        for (String name : names) {
            VehicleInfo info = vehiclesByName.get(name);
            if (info != null) {
                result.add(info);
            }
        }
        return Collections.enumeration(result);
    }

    // Applies a VehicleCreatedEvent to the read model, adding a new vehicle and indexing its position.
    public void apply(VehicleCreatedEvent event) {
        VehicleInfo info = new VehicleInfo(event.getName(), event.getStartPosition(), 0);
        vehiclesByName.put(event.getName(), info);
        indexPosition(info);
    }

    // Applies a VehicleMovedEvent to the read model, updating the vehicle's position and move count, and re-indexing its position.
    public void apply(VehicleMovedEvent event) {
        VehicleInfo current = vehiclesByName.get(event.getName());
        if (current == null) return;

        removeIndex(current);

        Position oldPos = current.getPosition();
        Position move = event.getMoveVector();
        Position newPos = new Position(oldPos.getX() + move.getX(), oldPos.getY() + move.getY());
        VehicleInfo updated = new VehicleInfo(current.getName(), newPos, current.getNumberOfMoves() + 1);
        vehiclesByName.put(updated.getName(), updated);
        indexPosition(updated);
    }

    // Applies a VehicleRemovedEvent to the read model, removing the vehicle and its position index.
    public void apply(VehicleRemovedEvent event) {
        VehicleInfo current = vehiclesByName.remove(event.getName());
        if (current != null) {
            removeIndex(current);
        }
    }

    // Helper method to index a vehicle's position for quick lookup of vehicles at that position.
    private void indexPosition(VehicleInfo info) {
        vehiclesByPosition.computeIfAbsent(info.getPosition(), p -> new HashSet<>()).add(info.getName());
    }

    // Helper method to remove a vehicle's position index when it moves or is removed.
    private void removeIndex(VehicleInfo info) {
        Set<String> names = vehiclesByPosition.get(info.getPosition());
        if (names != null) {
            names.remove(info.getName());
            if (names.isEmpty()) {
                vehiclesByPosition.remove(info.getPosition());
            }
        }
    }
}
