package v2.write;

import v2.domain.IVehicleEvent;

import java.util.List;

// Interface representing a repository for loading events related to vehicles,
// which can be implemented by various classes to provide access to the event store (e.g., in-memory, database, etc.) for reconstructing vehicle aggregates.
public interface IEventStoreRepository {
    List<IVehicleEvent> loadEventsForVehicle(String name);
}