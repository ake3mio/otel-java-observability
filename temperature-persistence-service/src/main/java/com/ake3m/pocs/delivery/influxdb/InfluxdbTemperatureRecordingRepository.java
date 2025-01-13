package com.ake3m.pocs.delivery.influxdb;

import com.ake3m.pocs.domain.TemperatureRecordingRepository;
import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.entity.Traceable;
import com.ake3m.pocs.telemetry.Telemetry;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.List;

public class InfluxdbTemperatureRecordingRepository implements TemperatureRecordingRepository {
    private static final String URL = "http://localhost:8086";
    private static final String TOKEN = "admin123";
    private static final String ORG = "foo";
    private static final String BUCKET = "foo";
    private static final Logger log = LoggerFactory.getLogger(InfluxdbTemperatureRecordingRepository.class);
    private final Telemetry telemetry;
    private final WriteApi writeApi;

    public InfluxdbTemperatureRecordingRepository(InfluxDBClient influxDBClient, Telemetry telemetry) {
        writeApi = influxDBClient.makeWriteApi();
        this.telemetry = telemetry;
    }

    @Override
    public void save(Traceable<TemperatureRecording> temperatureRecording) {
        telemetry.usingTraceContext(temperatureRecording, () -> {
            var taskName = "SAVE_TEMPERATURE_RECORDING";
            telemetry.profile(taskName, () -> {
                telemetry.trace(taskName, () -> {
                    var message = "Saving temperature recordings for: " + temperatureRecording.value().city().name();
                    log.info(message);
                    telemetry.addSpanEvent(message);
                    var temperatures = getTemperatures(temperatureRecording);
                    writeApi.writePoints(BUCKET, ORG, temperatures);
                    writeApi.flush();
                });
            });
        });
    }

    private List<Point> getTemperatures(Traceable<TemperatureRecording> temperatureRecording) {
        var city = temperatureRecording.value().city();
        return temperatureRecording.value().temperatures().stream()
                                   .map(result -> {
                                       var epochMillis = result.time()
                                                               .toInstant(ZoneOffset.UTC)
                                                               .toEpochMilli();
                                       return Point.measurement("temperature")
                                                   .addTag("city", city.name())
                                                   .addField("city", city.name())
                                                   .addField("country", city.country())
                                                   .addField("latitude", city.latitude())
                                                   .addField("longitude", city.longitude())
                                                   .addField("temperature", result.temperature())
                                                   .time(epochMillis, WritePrecision.MS);
                                   })
                                   .toList();
    }
}
