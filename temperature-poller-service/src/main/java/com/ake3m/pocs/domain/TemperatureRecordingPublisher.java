package com.ake3m.pocs.domain;

import com.ake3m.pocs.entity.TemperatureRecording;

public interface TemperatureRecordingPublisher {
    void publish(TemperatureRecording recording);
}
