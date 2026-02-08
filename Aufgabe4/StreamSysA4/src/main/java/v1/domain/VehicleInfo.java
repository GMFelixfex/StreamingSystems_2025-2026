package v1.domain;

// Data Transfer Object representing the current state of a vehicle, including its name, position, and number of moves made.
public class VehicleInfo implements IVehicleDTO {
    private final String name;
    private final Position position;
    private final int numberOfMoves;

    public VehicleInfo(String name, Position position, int numberOfMoves) {
        this.name = name;
        this.position = position;
        this.numberOfMoves = numberOfMoves;
    }

    @Override
    public String getName() { return name; }

    @Override
    public Position getPosition() { return position; }

    @Override
    public int getNumberOfMoves() { return numberOfMoves; }
}