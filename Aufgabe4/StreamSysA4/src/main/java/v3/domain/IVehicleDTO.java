package v3.domain;

// Data Transfer Object (DTO) interface for representing the state of a vehicle, including its name, position, and number of moves.
public interface IVehicleDTO {
    String getName();
    Position getPosition();
    int getNumberOfMoves();
}