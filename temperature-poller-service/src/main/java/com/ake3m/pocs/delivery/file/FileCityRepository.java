package com.ake3m.pocs.delivery.file;

import com.ake3m.pocs.domain.CityRepository;
import com.ake3m.pocs.entity.City;
import com.ake3m.pocs.telemetry.Telemetry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class FileCityRepository implements CityRepository {
    private final ObjectMapper objectMapper;
    private final Telemetry telemetry;
    private final List<City> cities;

    FileCityRepository(ObjectMapper objectMapper, Telemetry telemetry) {
        this.objectMapper = objectMapper;
        this.telemetry = telemetry;
        try (var stream = FileCityRepository.class.getResourceAsStream("/cities.json")) {
             cities = objectMapper.readValue(stream, new TypeReference<List<City>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<City> getAllCities() {
        try {
            return telemetry.trace("getAllCities", () -> {
                return cities;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
