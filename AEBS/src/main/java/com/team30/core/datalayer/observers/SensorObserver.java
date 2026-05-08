package com.team30.core.datalayer.observers;

import com.team30.core.datalayer.data.ProcessedSensorData;

public interface SensorObserver {
    void update(ProcessedSensorData data);
}
