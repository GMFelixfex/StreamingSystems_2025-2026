package v3.write;

import v3.domain.IVehicleEvent;

// Interface representing an event producer that can send IVehicleEvent messages,
// which can be implemented by various classes such as EventProducer for Kafka or InMemoryEventProducer for testing.
public interface IEventProducer {
    void send(IVehicleEvent event);
}