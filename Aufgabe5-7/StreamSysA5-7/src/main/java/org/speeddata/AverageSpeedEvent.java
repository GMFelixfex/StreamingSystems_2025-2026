package org.speeddata;

public class AverageSpeedEvent {
    private String sensorId;
    private double avgSpeedKmH;
    private long maxTs;

    // Standard-Konstruktor und Getter/Setter für Esper erforderlich
    public AverageSpeedEvent() {}
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public double getAvgSpeedKmH() { return avgSpeedKmH; }
    public void setAvgSpeedKmH(double avgSpeedKmH) { this.avgSpeedKmH = avgSpeedKmH; }
    public long getMaxTs() { return maxTs; }
    public void setMaxTs(long maxTs) { this.maxTs = maxTs; }
}
