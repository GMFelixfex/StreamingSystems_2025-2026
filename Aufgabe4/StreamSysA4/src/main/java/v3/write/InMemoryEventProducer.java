package v3.write;

import v3.domain.IVehicleEvent;

import java.util.ArrayList;
import java.util.List;

// In-memory implementation of the IEventProducer interface, which simply stores the produced events in a list for testing purposes.
public class InMemoryEventProducer implements IEventProducer {

    private final List<IVehicleEvent> events = new ArrayList<>();

    // Sends an IVehicleEvent by adding it to the in-memory list of events, allowing for retrieval and testing without external dependencies.
    @Override
    public void send(IVehicleEvent event) {
        events.add(event);
    }

    // Retrieves the list of events that have been sent to this in-memory event producer, which can be used for testing and verification purposes.
    public List<IVehicleEvent> getEvents() {
        return events;
    }
}