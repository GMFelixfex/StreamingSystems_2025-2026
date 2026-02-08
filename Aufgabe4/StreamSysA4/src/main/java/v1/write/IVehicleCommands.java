package v1.write;

import v1.domain.Position;

// Interface defining the contract for vehicle command operations, including creating, moving, and removing vehicles.
public interface IVehicleCommands {
    void createVehicle(String name, Position startPosition) throws Exception;
    void moveVehicle(String name, Position moveVector) throws Exception;
    void removeVehicle(String name) throws Exception;
}