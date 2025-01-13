package com.ake3m.pocs.domain;

import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.entity.Traceable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class TemperatureRecordingSubscriber {
    private static final Logger log = LoggerFactory.getLogger(TemperatureRecordingSubscriber.class);
    private final TemperatureRecordingRepository repository;
    private final ExecutorService executorService;
    private final BlockingQueue<Traceable<TemperatureRecording>> queue = new LinkedBlockingQueue<>();

    public TemperatureRecordingSubscriber(TemperatureRecordingRepository repository, ExecutorService executorService) {
        this.repository = repository;
        this.executorService = executorService;
        run();
    }

    public void onTemperatureRecording(Traceable<TemperatureRecording> temperatureRecording) {
        queue.add(temperatureRecording);
    }

    private void run() {
        executorService.execute(() -> {
            try {
                while (true) {
                    var temperatureRecordingTraceable = queue.take();
                    repository.save(temperatureRecordingTraceable);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Temperature recording subscriber interrupted");
            }
        });
    }
}
