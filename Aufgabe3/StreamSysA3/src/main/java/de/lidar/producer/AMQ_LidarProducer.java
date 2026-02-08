package de.lidar.producer;

import de.lidar.model.LidarPoint;
import de.lidar.util.UtilFunctions;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;
import java.io.*;

public class AMQ_LidarProducer {
    public static void main(String[] args) throws Exception {
        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("admin", "password","tcp://localhost:61616");
        cf.setTrustAllPackages(true);
        try (Connection conn = cf.createConnection();
             Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Topic topic = session.createTopic("Scans");
            MessageProducer producer = session.createProducer(topic);

            File file = new File("data\\Lidar-scans.txt");
            if (!file.exists()) { System.out.println("Datei nicht gefunden!"); return; }

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains("winkel")) continue;
                LidarPoint p = UtilFunctions.parseLine(line);
                ObjectMessage msg = session.createObjectMessage(p);
                producer.send(msg);
            }
            System.out.println("Alle Daten gesendet.");
        }
    }


}