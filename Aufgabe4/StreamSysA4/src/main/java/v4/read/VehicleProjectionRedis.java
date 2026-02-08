package v4.read;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import redis.clients.jedis.Jedis;
import v4.domain.*;

import java.time.Duration;
import java.util.Collections;

public class VehicleProjectionRedis implements Runnable {

    private final KafkaConsumer<String, IVehicleEvent> consumer;
    private final Jedis jedis;

    public VehicleProjectionRedis(KafkaConsumer<String, IVehicleEvent> consumer, Jedis jedis) {
        this.consumer = consumer;
        this.jedis = jedis;
    }

    @Override
    public void run() {
        consumer.subscribe(Collections.singletonList("vehicle-events"));
        System.out.println("Redis projection started, subscribed to vehicle-events");

        while (true) {
            var records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, IVehicleEvent> record : records) {
                IVehicleEvent event = record.value();
                if (event == null) continue;

                if (event instanceof VehicleCreatedEvent e) {
                    handleCreated(e);
                } else if (event instanceof VehicleMovedEvent e) {
                    handleMoved(e);
                } else if (event instanceof VehicleRemovedEvent e) {
                    handleRemoved(e);
                }
            }
        }
    }

    private void handleCreated(VehicleCreatedEvent e) {
        VehicleInfo info = new VehicleInfo(e.getName(), e.getStartPosition(), 0);
        save(info);
    }

    private void handleMoved(VehicleMovedEvent e) {
        VehicleInfo current = load(e.getName());
        if (current == null) return;

        Position oldPos = current.getPosition();
        Position move = e.getMoveVector();
        Position newPos = new Position(oldPos.getX() + move.getX(), oldPos.getY() + move.getY());
        VehicleInfo updated = new VehicleInfo(current.getName(), newPos, current.getNumberOfMoves() + 1);
        save(updated);
    }

    private void handleRemoved(VehicleRemovedEvent e) {
        String key = vehicleKey(e.getName());
        jedis.del(new String[]{key});
    }

    private String vehicleKey(String name) {
        return "vehicle:" + name;
    }

    private void save(VehicleInfo info) {
        String key = vehicleKey(info.getName());
        String value = serialize(info);
        jedis.set(key, value);
    }

    private VehicleInfo load(String name) {
        String key = vehicleKey(name);
        byte[] bytes = jedis.get(key.getBytes());
        if (bytes == null) return null;
        String value = new String(bytes);
        return deserialize(value);
    }


    private String serialize(VehicleInfo info) {
        return "{\"name\":\"" + info.getName() +
                "\",\"x\":" + info.getPosition().getX() +
                ",\"y\":" + info.getPosition().getY() +
                ",\"moves\":" + info.getNumberOfMoves() + "}";
    }

    private VehicleInfo deserialize(String json) {
        String[] parts = json.replace("{", "")
                .replace("}", "")
                .replace("\"", "")
                .split(",");

        String name = null;
        int x = 0, y = 0, moves = 0;
        for (String part : parts) {
            String[] kv = part.split(":");
            switch (kv[0]) {
                case "name" -> name = kv[1];
                case "x" -> x = Integer.parseInt(kv[1]);
                case "y" -> y = Integer.parseInt(kv[1]);
                case "moves" -> moves = Integer.parseInt(kv[1]);
            }
        }
        Position pos = new Position(x, y);
        return new VehicleInfo(name, pos, moves);
    }
}

