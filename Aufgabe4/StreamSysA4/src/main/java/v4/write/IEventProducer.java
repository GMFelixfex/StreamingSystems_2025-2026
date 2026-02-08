package v4.write;

import v4.domain.IVehicleEvent;

// Interface representing an event producer that can send IVehicleEvent messages,
// which can be implemented by various classes such as EventProducer for Kafka or InMemoryEventProducer for testing.
public interface IEventProducer {
    void send(IVehicleEvent event);
}