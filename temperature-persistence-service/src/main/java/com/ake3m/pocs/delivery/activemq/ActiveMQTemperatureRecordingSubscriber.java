package com.ake3m.pocs.delivery.activemq;

import com.ake3m.pocs.domain.TemperatureRecordingSubscriber;
import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.telemetry.Telemetry;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveMQTemperatureRecordingSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(ActiveMQTemperatureRecordingSubscriber.class);
    private final JMSConsumer consumer;
    private final Telemetry telemetry;
    private final Destination destination;
    private final TemperatureRecordingSubscriber temperatureRecordingSubscriber;

    public ActiveMQTemperatureRecordingSubscriber(JMSContext context, Destination destination, TemperatureRecordingSubscriber temperatureRecordingSubscriber, Telemetry telemetry) {
        this.destination = destination;
        this.temperatureRecordingSubscriber = temperatureRecordingSubscriber;
        this.consumer = context.createConsumer(destination);
        this.telemetry = telemetry;
    }

    public void start() {
        consumer.setMessageListener(this);
    }

    public void close() {
        consumer.close();
    }

    @Override
    public void onMessage(Message message) {
        telemetry.usingTraceContext(message, () -> consume(message));
    }

    private void consume(Message message) {
        var taskName = "RECEIVE_NAME_TEMPERATURE";
        telemetry.profile(taskName, () -> {
            this.telemetry.traceConsumer(taskName, destination.toString(), () -> {
                try {
                    log.info("received temperature recording message");
                    var temperatureRecording = message.getBody(TemperatureRecording.class);
                    temperatureRecordingSubscriber.onTemperatureRecording(telemetry.toTraceable(temperatureRecording));
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }
}
