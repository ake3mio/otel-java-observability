package com.ake3m.pocs.entity;

import java.io.Serializable;

public record City(String name, String country, double latitude, double longitude) implements Serializable {
}
