package v4.domain;

import java.io.Serializable;

// Interface representing a vehicle event, which can be implemented by various event types such as creation, movement, and removal of vehicles.
public interface IVehicleEvent extends Serializable {
    String getName();
}
