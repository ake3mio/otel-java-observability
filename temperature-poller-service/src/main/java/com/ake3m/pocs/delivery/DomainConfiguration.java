package com.ake3m.pocs.delivery;

import com.ake3m.pocs.domain.CityRepository;
import com.ake3m.pocs.domain.TemperatureChecksService;
import com.ake3m.pocs.domain.TemperatureClient;
import com.ake3m.pocs.domain.TemperatureRecordingPublisher;
import com.ake3m.pocs.telemetry.Telemetry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class DomainConfiguration {
    private final CityRepository cityRepository;
    private final TemperatureClient temperatureClient;
    private final TemperatureRecordingPublisher temperatureRecordingPublisher;
    private final Telemetry telemetry;

    public DomainConfiguration(
            CityRepository cityRepository,
            TemperatureClient temperatureClient,
            TemperatureRecordingPublisher temperatureRecordingPublisher,
            Telemetry telemetry) {
        this.cityRepository = cityRepository;
        this.temperatureClient = temperatureClient;
        this.temperatureRecordingPublisher = temperatureRecordingPublisher;
        this.telemetry = telemetry;
    }

    public TemperatureChecksService getTemperatureChecksService() {
        Supplier<ExecutorService> executorServiceFactory = () -> telemetry.wrap(Executors.newVirtualThreadPerTaskExecutor());
        return new TemperatureChecksService(
                cityRepository,
                temperatureClient,
                temperatureRecordingPublisher,
                executorServiceFactory);
    }
}
