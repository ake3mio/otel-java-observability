package com.ake3m.pocs.delivery.activemq;

import com.ake3m.pocs.domain.TemperatureRecordingSubscriber;
import com.ake3m.pocs.telemetry.Telemetry;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class ActiveMQConfiguration {
    private final String applicationName;
    private final TemperatureRecordingSubscriber temperatureRecordingSubscriber;
    private final Telemetry telemetry;

    public ActiveMQConfiguration(String applicationName, TemperatureRecordingSubscriber temperatureRecordingSubscriber, Telemetry telemetry) {
        this.applicationName = applicationName;
        this.temperatureRecordingSubscriber = temperatureRecordingSubscriber;
        this.telemetry = telemetry;
    }

    public ActiveMQTemperatureRecordingSubscriber getTemperatureRecordingSubscriber() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        connectionFactory.setClientID(applicationName);
        JMSContext context = connectionFactory.createContext();
        context.start();
        Queue queue = context.createQueue("TEMPERATURE");
        return new ActiveMQTemperatureRecordingSubscriber(context, queue, temperatureRecordingSubscriber, telemetry);
    }
}
