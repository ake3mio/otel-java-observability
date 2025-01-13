package com.ake3m.pocs.delivery.activemq;

import com.ake3m.pocs.domain.TemperatureRecordingPublisher;
import com.ake3m.pocs.telemetry.Telemetry;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import static com.ake3m.pocs.delivery.ApplicationConfiguration.APPLICATION_NAME;

public class ActiveMQConfiguration {
    private final Telemetry telemetry;

    public ActiveMQConfiguration(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    public TemperatureRecordingPublisher getTemperatureRecordingPublisher() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connectionFactory.setClientID(APPLICATION_NAME);
        JMSContext context = connectionFactory.createContext();
        context.start();
        Queue queue = context.createQueue("TEMPERATURE");
        JMSProducer producer = context.createProducer();
        return new ActiveMQTemperatureRecordingPublisher(context, queue, producer, telemetry);
    }
}
