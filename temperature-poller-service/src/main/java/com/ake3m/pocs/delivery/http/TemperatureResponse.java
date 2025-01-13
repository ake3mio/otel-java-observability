package com.ake3m.pocs.delivery.http;

import java.time.LocalDateTime;
import java.util.List;

public record TemperatureResponse(
        double latitude,
        double longitude,
        Hourly hourly
) {
    record Hourly(
            List<LocalDateTime> time,
            List<Double> temperature_2m
    ){}
}
