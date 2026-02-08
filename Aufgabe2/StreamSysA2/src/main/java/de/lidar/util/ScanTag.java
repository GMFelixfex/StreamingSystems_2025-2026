package de.lidar.util;
import de.lidar.model.LidarPoint;

public class ScanTag {
    double lastAngle;
    double currentScanNr;

    public ScanTag() {
        lastAngle = -1;
        currentScanNr = 1;
    }

    public LidarPoint Check(LidarPoint p) {
        double angle = p.winkel;
        if (angle < lastAngle) {
            currentScanNr++;
        }
        lastAngle = angle;
        p.scanNr = (int) currentScanNr;
        return p;
    }
}
