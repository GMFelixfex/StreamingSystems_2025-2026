package v2.write;

import v2.domain.IVehicleEvent;

import java.util.ArrayList;
import java.util.List;

// Implementation of the IEventStoreRepository interface that uses an InMemoryEventProducer to load events for a specific vehicle,
// allowing for testing and reconstruction of vehicle aggregates without relying on external event stores like Kafka.
public class EventStoreRepository implements IEventStoreRepository {

    private final InMemoryEventProducer producer;

    public EventStoreRepository(InMemoryEventProducer producer) {
        this.producer = producer;
    }

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
