package org.speeddata;

public class KafkaMain {

    private static final String BOOTSTRAP = "localhost:9092";
    private static final String TOPIC = "speeddata";
    private static final String DATA_FILE = "data/Trafficdata.txt";
    private static final String OUTPUT_FILE = "data/averages";


    public static void main(String[] args) throws Exception {
        new KafkaMain().run();
    }

    public void run() throws Exception {
        // 1) (TEST) Clear topic contents before producing new test data
        KafkaTopicStats.clearTopic(BOOTSTRAP, TOPIC);

        // 2) Produce the lines from the data file into Kafka (each record's timestamp set to the event time)
        KafkaFileProducer producer = new KafkaFileProducer(BOOTSTRAP, TOPIC);
        producer.publishFile(DATA_FILE);
        producer.close();

        // 3) Print approximate message count in the topic so user can see progress
        KafkaTopicStats.printTopicCount(BOOTSTRAP, TOPIC);

        // 4) Quick standalone consumer to verify group creation/connectivity
        KafkaConsumerTest.pollGroup(BOOTSTRAP, TOPIC, "speeddata-beam-consumer-test", 5000);

        // 5) Run the Esper CEP engine that reads from Kafka, computes per-sensor average speed (km/h) in 10-second windows, and writes results to a file
        KafkaCepRunnerExtended.run(BOOTSTRAP, TOPIC);

        // 6) Run the Beam pipeline that reads from Kafka, computes per-sensor average speed (km/h), and writes results to a file
        KafkaBeamPipeline.run(BOOTSTRAP, TOPIC);



    }
}