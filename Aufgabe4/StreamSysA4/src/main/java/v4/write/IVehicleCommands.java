package v4.write;

import v4.domain.Position;

// Interface representing the commands that can be executed on a vehicle, such as creating, moving, or removing a vehicle,
// which can be implemented by various classes to handle the business logic of these operations.
public interface IVehicleCommands {
    void createVehicle(String name, Position startPosition) throws Exception;
    void moveVehicle(String name, Position moveVector) throws Exception;
    void removeVehicle(String name) throws Exception;
}