package v3.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import v3.domain.IVehicleEvent;

// Custom Kafka serializer for IVehicleEvent, which can handle multiple event types (created, moved, removed)
public class VehicleEventSerializer implements Serializer<IVehicleEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, IVehicleEvent data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
