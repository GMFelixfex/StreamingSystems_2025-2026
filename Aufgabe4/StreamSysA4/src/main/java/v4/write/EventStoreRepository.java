package v4.write;

import v4.domain.IVehicleEvent;

import java.util.ArrayList;
import java.util.List;

// Implementation of the IEventStoreRepository interface that retrieves events for a specific vehicle from an in-memory event producer.
public class EventStoreRepository implements IEventStoreRepository {

    private final InMemoryEventProducer producer;

    public EventStoreRepository(InMemoryEventProducer producer) {
        this.producer = producer;
    }

    // Loads all events for a given vehicle name by filtering the events stored in the in-memory event producer.
    @Override
    public List<IVehicleEvent> loadEventsForVehicle(String name) {
        List<IVehicleEvent> result = new ArrayList<>();
        for (IVehicleEvent event : producer.getEvents()) {
            if (name.equals(event.getName())) {
                result.add(event);
            }
        }
        return result;
    }
}
