package v2.domain;

// Event representing the creation of a new vehicle with a name and starting position.
public class VehicleCreatedEvent implements IVehicleEvent {
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
