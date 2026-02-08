package v1.read;

import v1.domain.IVehicleDTO;
import v1.domain.Position;

import java.util.Enumeration;

// Interface defining the query operations for retrieving vehicle information from the read model.
public interface IQuery {
    public IVehicleDTO getVehicleByName(String name);
    public Enumeration<IVehicleDTO> getVehicles();
    public Enumeration<IVehicleDTO> getVehiclesAtPosition(Position position);
}