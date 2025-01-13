package com.ake3m.pocs.domain;

import com.ake3m.pocs.entity.City;
import com.ake3m.pocs.entity.TemperatureRecording;

import java.util.Optional;

public interface TemperatureClient {
    Optional<TemperatureRecording> getTemperatureRecording(City city);
}
