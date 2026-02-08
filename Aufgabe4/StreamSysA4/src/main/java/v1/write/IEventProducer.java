package v1.write;

import v1.domain.IVehicleEvent;

// Interface defining the contract for an event producer that can send vehicle events to the event store or message broker.
public interface IEventProducer {
    void send(IVehicleEvent event);
}