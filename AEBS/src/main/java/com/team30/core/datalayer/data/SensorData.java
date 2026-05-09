package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

public abstract class SensorData {
    private final SensorId sensorId;
    private final long timestampMs;
    private final boolean isGarbage;

    protected SensorData(SensorId sensorId, long timestampMs, boolean isGarbage) {
        this.sensorId = sensorId;
        this.timestampMs = timestampMs;
        this.isGarbage = isGarbage;
    }

    public abstract SensorType getSensorType();

    public SensorId getSensorId()  { return sensorId; }
    public long getTimestampMs()   { return timestampMs; }
    public boolean isGarbage()     { return isGarbage; }
}