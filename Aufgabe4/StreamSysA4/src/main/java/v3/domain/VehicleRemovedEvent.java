package v3.domain;

import java.io.Serial;

// Event representing the removal of a vehicle, containing only the vehicle's name.
public class VehicleRemovedEvent implements IVehicleEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;

    public VehicleRemovedEvent(String name) {
        this.name = name;
    }

    @Override
    public String getName() { return name; }
}
