package v2.write;

import v2.domain.IVehicleEvent;

import java.util.ArrayList;
import java.util.List;

// In-memory implementation of the IEventProducer interface, which simply stores the produced events in a list for testing purposes.
public class InMemoryEventProducer implements IEventProducer {

    private final List<IVehicleEvent> events = new ArrayList<>();

    @Override
    public void send(IVehicleEvent event) {
        events.add(event);
    }

    public List<IVehicleEvent> getEvents() {
        return events;
    }
}