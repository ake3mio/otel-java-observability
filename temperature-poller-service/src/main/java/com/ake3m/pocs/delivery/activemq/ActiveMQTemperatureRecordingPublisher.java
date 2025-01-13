package com.ake3m.pocs.delivery.activemq;

import com.ake3m.pocs.domain.TemperatureRecordingPublisher;
import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.telemetry.Telemetry;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.ObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ActiveMQTemperatureRecordingPublisher implements TemperatureRecordingPublisher {
    private static final Logger log = LoggerFactory.getLogger(ActiveMQTemperatureRecordingPublisher.class);
    private final JMSContext context;
    private final Destination destination;
    private final JMSProducer producer;
    private final Telemetry telemetry;
    private final Lock mutex = new ReentrantLock();

    public ActiveMQTemperatureRecordingPublisher(
            JMSContext context,
            Destination destination,
            JMSProducer producer,
            Telemetry telemetry) {
        this.context = context;
        this.destination = destination;
        this.producer = producer;
        this.telemetry = telemetry;
    }

    @Override
    public void publish(TemperatureRecording recording) {
        mutex.lock();
        try {
            var taskName = "PUBLISH_TEMPERATURE_RECORDING";
            telemetry.profile(taskName, () -> {
                telemetry.traceProducer(taskName, "temperature-persistence-service", destination.toString(), () -> {
                    telemetry.addSpanEvent("Publishing recording for: " + recording.city().name());
                    ObjectMessage message = context.createObjectMessage(recording);
                    telemetry.injectTraceContext(message);
                    producer.send(destination, message);
                });
            });
        } finally {
            mutex.unlock();
        }
    }
}
