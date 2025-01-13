package com.ake3m.pocs;

import com.ake3m.pocs.delivery.DomainConfiguration;
import com.ake3m.pocs.delivery.activemq.ActiveMQConfiguration;
import com.ake3m.pocs.delivery.activemq.ActiveMQTemperatureRecordingSubscriber;
import com.ake3m.pocs.delivery.influxdb.InfluxdbConfiguration;
import com.ake3m.pocs.telemetry.TelemetryConfiguration;
import com.ake3m.pocs.telemetry.TelemetryProperties;
import io.javalin.Javalin;
import io.javalin.micrometer.MicrometerPlugin;

public class TemperaturePersistenceServiceApplication {

    public static void main(String[] args) {
        var applicationName = "temperature-persistence-service";

        var telemetryConfiguration = TelemetryConfiguration.create(new TelemetryProperties(
                "http://localhost:8888",
                "http://localhost:4320",
                applicationName
        ));

        var registry = telemetryConfiguration.getMeterRegistry();
        var activeMQTemperatureRecordingSubscriber = getActiveMQTemperatureRecordingSubscriber(telemetryConfiguration, applicationName);

        activeMQTemperatureRecordingSubscriber.start();

        MicrometerPlugin micrometerPlugin = new MicrometerPlugin(micrometerPluginConfig -> micrometerPluginConfig.registry = registry);
        Javalin app = Javalin.create(config -> config.registerPlugin(micrometerPlugin)).start(8081);
        app.get("/metrics", ctx -> {
            ctx.contentType("application/openmetrics-text; version=1.0.0; charset=utf-8")
               .result(registry.scrape("application/openmetrics-text"));
        });
    }

    private static ActiveMQTemperatureRecordingSubscriber getActiveMQTemperatureRecordingSubscriber(TelemetryConfiguration telemetryConfiguration, String applicationName) {
        var telemetry = telemetryConfiguration.getTelemetry();
        var influxdbConfiguration = new InfluxdbConfiguration(telemetry);
        var temperatureRecordingRepository = influxdbConfiguration.getTemperatureRecordingRepository();
        var domainConfiguration = new DomainConfiguration(temperatureRecordingRepository, telemetry);
        var temperatureRecordingSubscriber = domainConfiguration.getTemperatureRecordingSubscriber();
        return new ActiveMQConfiguration(applicationName, temperatureRecordingSubscriber, telemetry).getTemperatureRecordingSubscriber();
    }
}
