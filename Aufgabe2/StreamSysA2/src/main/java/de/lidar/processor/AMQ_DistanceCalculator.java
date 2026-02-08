package de.lidar.processor;

import de.lidar.model.LidarPoint;
import de.lidar.util.UtilFunctions;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;

public class AMQ_DistanceCalculator implements MessageListener {
    private LidarPoint lastPoint = null;
    private MessageProducer producer;
    private Session session;
    private final int MIN_QUALITY = 15;

    public static void main(String[] args) throws Exception {
        new AMQ_DistanceCalculator().start();
    }

    public void start() throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("admin", "password","tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        Connection conn = cf.createConnection();
        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(session.createTopic("Distanzen"));
        session.createConsumer(session.createTopic("TaggedScans")).setMessageListener(this);
        conn.start();
        System.out.println("AMQ_DistanceCalculator aktiv. Drücken Sie Enter zum Beenden...");
        System.in.read();
        conn.close();
    }

    @Override
    public void onMessage(Message m) {
        try {
            LidarPoint current = (LidarPoint) ((ObjectMessage)m).getObject();
            if (current.qualitaet < MIN_QUALITY) return;

            // WICHTIG: Nur berechnen, wenn im selben Scan und lastPoint existiert
            if (lastPoint != null && lastPoint.scanNr == current.scanNr) {
                double d = UtilFunctions.calcDistance(lastPoint, current);
                MapMessage out = session.createMapMessage();
                out.setInt("scan", current.scanNr);
                out.setDouble("dist", d);
                producer.send(out);
            }
            lastPoint = current;
        } catch (Exception e) { e.printStackTrace(); }
    }
}