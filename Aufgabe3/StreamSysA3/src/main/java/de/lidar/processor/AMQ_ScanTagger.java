package de.lidar.processor;

import de.lidar.model.LidarPoint;
import de.lidar.util.ScanTag;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;

public class AMQ_ScanTagger implements MessageListener {
    private ScanTag scanTag = new ScanTag();
    private MessageProducer producer;
    private Session session;

    public static void main(String[] args) throws Exception {
        new AMQ_ScanTagger().start();
    }

    public void start() throws Exception {
        ActiveMQConnectionFactory  cf = new ActiveMQConnectionFactory("admin", "password","tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        Connection conn = cf.createConnection();
        session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(session.createTopic("TaggedScans"));
        session.createConsumer(session.createTopic("Scans")).setMessageListener(this);
        conn.start();
        System.out.println("AMQ_ScanTagger aktiv. Drücken Sie Enter zum Beenden...");
        System.in.read();
        conn.close();
    }

    @Override
    public void onMessage(Message m) {
        try {
            LidarPoint p = (LidarPoint) ((ObjectMessage)m).getObject();
            p = scanTag.Check(p);
            producer.send(session.createObjectMessage(p));
        } catch (Exception e) { e.printStackTrace(); }
    }
}