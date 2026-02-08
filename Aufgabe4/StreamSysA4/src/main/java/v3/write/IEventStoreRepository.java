package v3.write;

import v3.domain.IVehicleEvent;

import java.util.List;

// Interface representing a repository for storing and retrieving vehicle events,
// which can be implemented by various classes such as EventStoreRepository for in-memory storage or a database-backed implementation for production use.
public interface IEventStoreRepository {
    List<IVehicleEvent> loadEventsForVehicle(String name);
}