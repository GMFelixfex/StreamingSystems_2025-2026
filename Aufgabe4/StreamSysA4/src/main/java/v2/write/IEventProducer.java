package v2.write;

import v2.domain.IVehicleEvent;

public interface IEventProducer {
    void send(IVehicleEvent event);
}