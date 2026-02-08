package org.speeddata;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class SpeedEvent {
    private final Instant timestamp;
    private final String sensorId;
    private final double speed; // m/s

    public SpeedEvent(Instant timestamp, String sensorId, double speed) {
        this.timestamp = timestamp;
        this.sensorId = sensorId;
        this.speed = speed;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSensorId() {
        return sensorId;
    }

    public double getSpeed() {
        return speed;
    }

    public static SpeedEvent parseFromString(String line) throws IllegalArgumentException {
        // Expected format: 2025-05-13T18:06:57.024Z 2 10.3
        if (line == null) throw new IllegalArgumentException("line is null");
        String s = line.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty line");
        String[] parts = s.split("\\s+");
        if (parts.length < 3) throw new IllegalArgumentException("invalid format: " + line);
        try {
            Instant ts = Instant.parse(parts[0]);
            String sensor = parts[1];
            double speed = Double.parseDouble(parts[2]);
            return new SpeedEvent(ts, sensor, speed);
        } catch (DateTimeParseException | NumberFormatException ex) {
            throw new IllegalArgumentException("failed to parse line: " + line, ex);
        }
    }

    public long getTimestampMillis() {
        return timestamp.toEpochMilli();
    }
}
