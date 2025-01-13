package com.ake3m.pocs;

import com.ake3m.pocs.delivery.ApplicationConfiguration;
import com.ake3m.pocs.delivery.DomainConfiguration;
import com.ake3m.pocs.delivery.activemq.ActiveMQConfiguration;
import com.ake3m.pocs.delivery.file.FileConfiguration;
import com.ake3m.pocs.delivery.http.HttpConfiguration;
import com.ake3m.pocs.delivery.quartz.QuartzConfiguration;
import com.ake3m.pocs.delivery.quartz.QuartzScheduler;
import com.ake3m.pocs.domain.TemperatureChecksService;
import com.ake3m.pocs.domain.TemperatureClient;
import com.ake3m.pocs.telemetry.Telemetry;
import com.ake3m.pocs.telemetry.TelemetryConfiguration;
import com.ake3m.pocs.telemetry.TelemetryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;
import io.micrometer.observation.ObservationRegistry;
import org.quartz.SchedulerException;

public class TemperaturePollerServiceApplication {
    public static void main(String[] args) throws SchedulerException {
        var telemetryConfiguration = TelemetryConfiguration.create(new TelemetryProperties(
                "http://localhost:8888",
                "http://localhost:4320",
                "temperature-poller-service"
        ));
        var applicationConfiguration = new ApplicationConfiguration();
        var observationRegistry = telemetryConfiguration.getObservationRegistry();
        var registry = telemetryConfiguration.getMeterRegistry();
        var telemetry = telemetryConfiguration.getTelemetry();
        var quartzScheduler = getQuartzScheduler(applicationConfiguration, observationRegistry, telemetry);

        quartzScheduler.start();

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        Javalin app = Javalin.create(config -> config.registerPlugin(micrometerPlugin)).start(8080);
        app.get("/metrics", ctx -> {
            ctx.contentType("application/openmetrics-text; version=1.0.0; charset=utf-8")
               .result(registry.scrape("application/openmetrics-text"));
        });

    }

    private static QuartzScheduler getQuartzScheduler(ApplicationConfiguration applicationConfiguration, ObservationRegistry observationRegistry, Telemetry telemetry) {
        var objectMapper = applicationConfiguration.getObjectMapper();
        var httpConfiguration = new HttpConfiguration(observationRegistry, telemetry, objectMapper);
        var temperatureClient = httpConfiguration.getTemperatureClient();
        var temperatureChecksService = getTemperatureChecksService(objectMapper, temperatureClient, telemetry);
        return new QuartzConfiguration(temperatureChecksService, telemetry).getQuartzScheduler();
    }

    private static TemperatureChecksService getTemperatureChecksService(ObjectMapper objectMapper, TemperatureClient temperatureClient, Telemetry telemetry) {
        var fileConfiguration = new FileConfiguration(objectMapper, telemetry);
        var activeMQConfiguration = new ActiveMQConfiguration(telemetry);
        var cityRepository = fileConfiguration.getCityRepository();
        var temperatureRecordingPublisher = activeMQConfiguration.getTemperatureRecordingPublisher();
        var domainConfiguration = new DomainConfiguration(cityRepository, temperatureClient, temperatureRecordingPublisher, telemetry);
        return domainConfiguration.getTemperatureChecksService();
    }
}
