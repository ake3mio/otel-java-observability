package com.ake3m.pocs.entity;

public record Traceable<T>(String traceid, T value) {
}
