package com.ake3m.pocs.delivery.http;

import com.ake3m.pocs.entity.City;
import com.ake3m.pocs.entity.Temperature;
import com.ake3m.pocs.entity.TemperatureRecording;

import java.util.ArrayList;
import java.util.List;

public class HttpResponseMapper {
    HttpResponseMapper() {
    }

    public TemperatureRecording map(City city, TemperatureResponse temperatureResponse) {
        var hourly = temperatureResponse.hourly();
        var temperatureValues = hourly.temperature_2m();
        var temperatureTimes = hourly.time();

        List<Temperature> temperatures = new ArrayList<>();
        for (int i = 0; i < temperatureValues.size(); i++) {
            temperatures.add(new Temperature(temperatureTimes.get(i), temperatureValues.get(i)));
        }

        return new TemperatureRecording(city, temperatures);
    }

}
