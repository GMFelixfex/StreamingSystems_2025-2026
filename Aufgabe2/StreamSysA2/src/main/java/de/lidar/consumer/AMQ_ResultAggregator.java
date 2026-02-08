package de.lidar.consumer;

import de.lidar.util.UtilFunctions;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;
import java.util.*;

public class AMQ_ResultAggregator implements MessageListener {
    private TreeMap<Integer, Double> totals = new TreeMap<>(); // TreeMap hält Scan-Reihenfolge

    public static void main(String[] args) throws Exception {
        new AMQ_ResultAggregator().start();
    }

    public void start() throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("admin", "password",
            "failover:(tcp://localhost:61616)?maxReconnectAttempts=-1&initialReconnectDelay=1000&useExponentialBackOff=true");
        cf.setTrustAllPackages(true);
        Connection conn = cf.createConnection();
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        session.createConsumer(session.createTopic("Distanzen")).setMessageListener(this);
        conn.start();

        System.out.println("Aggregator aktiv. Ergebnisse werden in results.json geschrieben... Drücken Sie Enter zum Beenden.");
        System.in.read();

        conn.close();
    }

    @Override
    public void onMessage(Message m) {
        try {
            MapMessage msg = (MapMessage)m;
            int scan = msg.getInt("scan");
            double d = msg.getDouble("dist");

            totals.put(scan, totals.getOrDefault(scan, 0.0) + d);
            UtilFunctions.writeResults(totals);
        } catch (Exception e) { e.printStackTrace(); }
    }


}