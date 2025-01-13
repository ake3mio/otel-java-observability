
package com.ake3m.pocs.delivery;

import com.ake3m.pocs.domain.TemperatureRecordingRepository;
import com.ake3m.pocs.domain.TemperatureRecordingSubscriber;
import com.ake3m.pocs.telemetry.Telemetry;

import java.util.concurrent.Executors;

public class DomainConfiguration {
    private final Telemetry telemetry;
    private final TemperatureRecordingRepository repository;

    public DomainConfiguration(TemperatureRecordingRepository repository, Telemetry telemetry) {
        this.repository = repository;
        this.telemetry = telemetry;
    }

    public TemperatureRecordingSubscriber getTemperatureRecordingSubscriber() {
        return new TemperatureRecordingSubscriber(repository, telemetry.wrap(Executors.newSingleThreadExecutor()));
    }
}
