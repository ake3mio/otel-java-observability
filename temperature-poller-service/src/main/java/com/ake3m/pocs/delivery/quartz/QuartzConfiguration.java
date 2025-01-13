package com.ake3m.pocs.delivery.quartz;

import com.ake3m.pocs.domain.TemperatureChecksService;
import com.ake3m.pocs.telemetry.Telemetry;

public class QuartzConfiguration {

    private final TemperatureChecksService temperatureChecksService;
    private final Telemetry telemetry;

    public QuartzConfiguration(TemperatureChecksService temperatureChecksService, Telemetry telemetry) {
        this.temperatureChecksService = temperatureChecksService;
        this.telemetry = telemetry;
    }

    public QuartzScheduler getQuartzScheduler() {
        return new QuartzScheduler(temperatureChecksService, telemetry);
    }
}
