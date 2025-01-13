package com.ake3m.pocs.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public record Temperature(LocalDateTime time, double temperature) implements Serializable {
}
