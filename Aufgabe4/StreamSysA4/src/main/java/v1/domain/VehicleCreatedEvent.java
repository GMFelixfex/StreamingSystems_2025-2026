package v1.domain;

import java.io.Serial;

// Event representing the creation of a new vehicle with a name and starting position.
public class VehicleCreatedEvent implements IVehicleEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Position startPosition;

    public VehicleCreatedEvent(String name, Position startPosition) {
        this.name = name;
        this.startPosition = startPosition;
    }

    @Override
    public String getName() { return name; }

    public Position getStartPosition() { return startPosition; }
}
