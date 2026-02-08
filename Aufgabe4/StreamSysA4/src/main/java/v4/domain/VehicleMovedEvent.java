package v4.domain;

import java.io.Serial;

// Event representing the movement of a vehicle, containing the vehicle's name and the vector by which it moved.
public class VehicleMovedEvent implements IVehicleEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Position moveVector;

    public VehicleMovedEvent(String name, Position moveVector) {
        this.name = name;
        this.moveVector = moveVector;
    }

    @Override
    public String getName() { return name; }

    public Position getMoveVector() { return moveVector; }
}
