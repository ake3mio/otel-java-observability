package com.ake3m.pocs.delivery.influxdb;

import com.ake3m.pocs.domain.TemperatureRecordingRepository;
import com.ake3m.pocs.telemetry.Telemetry;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;

public class InfluxdbConfiguration {
    private final Telemetry telemetry;

    public InfluxdbConfiguration(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    public TemperatureRecordingRepository getTemperatureRecordingRepository() {
        InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://localhost:8086", "admin123".toCharArray());
        return new InfluxdbTemperatureRecordingRepository(influxDBClient, telemetry);
    }
}
