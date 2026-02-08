package v2.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import v2.domain.IVehicleEvent;

// Custom Kafka serializer for IVehicleEvent, which can handle multiple event types (created, moved, removed)
// by serializing them with type information to ensure correct deserialization on the consumer side.
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
