package com.ake3m.pocs.entity;

import java.io.Serializable;
import java.util.List;

public record TemperatureRecording(City city, List<Temperature> temperatures) implements Serializable {
}
