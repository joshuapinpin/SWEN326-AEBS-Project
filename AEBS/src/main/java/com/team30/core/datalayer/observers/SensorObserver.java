package com.team30.core.datalayer.observers;

import com.team30.core.datalayer.data.SensorData;

public interface SensorObserver {
    void update(SensorData data);
}