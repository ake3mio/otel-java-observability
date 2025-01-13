package com.ake3m.pocs.delivery.http;

import com.ake3m.pocs.domain.TemperatureClient;
import com.ake3m.pocs.entity.City;
import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.telemetry.Telemetry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class HttpTemperatureClient implements TemperatureClient {
    private static final Logger log = LoggerFactory.getLogger(HttpTemperatureClient.class);
    private final HttpClient httpClient;
    private final Telemetry telemetry;
    private final ObjectMapper objectMapper;
    private final HttpResponseMapper mapper;
    private final String apiUrl;

    HttpTemperatureClient(ObjectMapper objectMapper,
                          HttpResponseMapper mapper,
                          HttpClient httpClient,
                          String apiUrl,
                          Telemetry telemetry) {
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.apiUrl = apiUrl;
        this.httpClient = httpClient;
        this.telemetry = telemetry;
    }

    @Override
    public Optional<TemperatureRecording> getTemperatureRecording(City city) {
        try {
            return telemetry.profile("getTemperatureRecording", () -> {
                telemetry.addSpanEvent("Get temperature recording for city: " + city.name());
                ClassicHttpRequest httpGet = ClassicRequestBuilder
                        .get(apiUrl)
                        .addParameter("latitude", String.valueOf(city.latitude()))
                        .addParameter("longitude", String.valueOf(city.longitude()))
                        .addParameter("hourly", "temperature_2m")
                        .addParameter("past_days", "1")
                        .build();
                try {
                    String response = httpClient.execute(httpGet, new BasicHttpClientResponseHandler());
                    var temperatureResponse = objectMapper.readValue(response, TemperatureResponse.class);
                    return Optional.of(mapper.map(city, temperatureResponse));
                } catch (Exception e) {
                    telemetry.addSpanError("Failed to get temperature recording for city: " + city.name(), e);
                    log.error("Failed to get temperature recording, because {}", e.getMessage(), e);
                    return Optional.empty();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
