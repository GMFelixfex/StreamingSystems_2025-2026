package de.lidar.util;

import de.lidar.model.LidarPoint;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class UtilFunctions {
    public static double calcDistance(LidarPoint p1, LidarPoint p2) {
        double a1 = Math.toRadians(p1.winkel);
        double a2 = Math.toRadians(p2.winkel);
        double x1 = p1.distanz * Math.cos(a1);
        double y1 = p1.distanz * Math.sin(a1);
        double x2 = p2.distanz * Math.cos(a2);
        double y2 = p2.distanz * Math.sin(a2);
        return Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
    }

    public static LidarPoint parseLine(String line) {
        line = line.replaceAll("[{}\" ]", "");
        String[] parts = line.split(",");
        double w = Double.parseDouble(parts[0].split(":")[1]);
        double d = Double.parseDouble(parts[1].split(":")[1]);
        int q = Integer.parseInt(parts[2].split(":")[1]);
        return new LidarPoint(w, d, q);
    }

    public synchronized static void writeResults(TreeMap<Integer, Double> totals) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter("results.txt"))) {
            for (Map.Entry<Integer, Double> e : totals.entrySet()) {
                pw.printf(Locale.US, "{\"scan\": %d, \"distance\": %.2f}%n", e.getKey(), e.getValue());
            }
        }
    }

}
