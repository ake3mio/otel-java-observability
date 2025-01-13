package com.ake3m.pocs.domain;

import com.ake3m.pocs.entity.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public class TemperatureChecksService {
    private static final Logger log = LoggerFactory.getLogger(TemperatureChecksService.class);
    private final CityRepository cityRepository;
    private final TemperatureClient temperatureClient;
    private final TemperatureRecordingPublisher temperatureRecordingPublisher;
    private final ExecutorService executorService;

    public TemperatureChecksService(
            CityRepository cityRepository,
            TemperatureClient temperatureClient,
            TemperatureRecordingPublisher temperatureRecordingPublisher,
            Supplier<ExecutorService> executorServiceFactory) {
        this.cityRepository = cityRepository;
        this.temperatureClient = temperatureClient;
        this.temperatureRecordingPublisher = temperatureRecordingPublisher;
        executorService = executorServiceFactory.get();
    }

    public void checkTemperatures() {
        var cities = cityRepository.getAllCities();
        var countDownLatch = new CountDownLatch(cities.size());
        cities.forEach(city -> {
            getAndPublishTemperature(city, countDownLatch);
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for temperature recordings", e);
            Thread.currentThread().interrupt();
        }
    }

    private void getAndPublishTemperature(City city, CountDownLatch countDownLatch) {
        executorService.submit(() -> {
            log.info("Getting temperature recording for {}", city);
            try {
                temperatureClient.getTemperatureRecording(city)
                        .ifPresent(recording -> {
                            log.info("Publishing temperature recording for {}", recording.city().name());
                            temperatureRecordingPublisher.publish(recording);
                        });
            } finally {
                countDownLatch.countDown();
            }
        });
    }
}
