package com.ake3m.pocs.domain;

import com.ake3m.pocs.entity.TemperatureRecording;
import com.ake3m.pocs.entity.Traceable;

public interface TemperatureRecordingRepository {
    void save(Traceable<TemperatureRecording> temperatureRecording);
}
