package de.lidar.model;
import java.io.Serializable;

public class LidarPoint implements Serializable {
    private static final long serialVersionUID = 1L;
    public double winkel;
    public double distanz;
    public int qualitaet;
    public int scanNr; // wird in Schritt 2 befüllt

    public LidarPoint() {}

    public LidarPoint(double winkel, double distanz, int qualitaet) {
        this.winkel = winkel;
        this.distanz = distanz;
        this.qualitaet = qualitaet;
        this.scanNr = 0; // Initialwert
    }

    @Override
    public String toString() {
        return String.format("Scan #%d: %.2f° | %.2fmm (Q: %d)", scanNr, winkel, distanz, qualitaet);
    }
}

