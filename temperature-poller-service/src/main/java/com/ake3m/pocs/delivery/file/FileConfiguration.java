package com.ake3m.pocs.delivery.file;

import com.ake3m.pocs.domain.CityRepository;
import com.ake3m.pocs.telemetry.Telemetry;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FileConfiguration {
    private final ObjectMapper objectMapper;
    private final Telemetry telemetry;

    public FileConfiguration(ObjectMapper objectMapper, Telemetry telemetry) {
        this.objectMapper = objectMapper;
        this.telemetry = telemetry;
    }

    public CityRepository getCityRepository() {
        return new FileCityRepository(objectMapper, telemetry);
    }
}
