package v3.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import v3.domain.*;

import java.util.Map;

// Custom Kafka deserializer for IVehicleEvent, which can handle multiple event types (created, moved, removed)
public class VehicleEventDeserializer implements Deserializer<IVehicleEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IVehicleEvent deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = objectMapper.readValue(data, Map.class);

            String name = (String) eventMap.get("name");

            if (eventMap.containsKey("startPosition")) {  // VehicleCreatedEvent has startPosition
                Map<String, Object> posMap = (Map<String, Object>) eventMap.get("startPosition");
                Position pos = new Position((Integer) posMap.get("x"), (Integer) posMap.get("y"));
                return new VehicleCreatedEvent(name, pos);
            } else if (eventMap.containsKey("moveVector")) {  // VehicleMovedEvent has moveVector
                Map<String, Object> posMap = (Map<String, Object>) eventMap.get("moveVector");
                Position pos = new Position((Integer) posMap.get("x"), (Integer) posMap.get("y"));
                return new VehicleMovedEvent(name, pos);
            } else {  // If neither startPosition nor moveVector is present, it's a VehicleRemovedEvent
                return new VehicleRemovedEvent(name);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing VehicleEvent", e);
        }
    }
}
