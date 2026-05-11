package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

/**
 * Base class for all sensor readings in the AEBS system.
 *
 * Stores metadata common to every sensor reading,
 * including the sensor identity, the time the reading was captured, and whether
 * the reading is considered garbage (invalid or produced by a failed sensor).
 */
public abstract class SensorData {
    private final SensorId sensorId;
    private final long timestampMs;
    private final boolean isGarbage;

    /**
     * Constructs a SensorData with the given metadata fields.
     * @param sensorId the identity of the sensor that produced this reading (PRIMARY or REDUNDANT)
     * @param timestampMs the time this reading was captured in milliseconds
     * @param isGarbage true if this reading is considered garbage (invalid or from a failed sensor), false otherwise
     */
    protected SensorData(SensorId sensorId, long timestampMs, boolean isGarbage) {
        this.sensorId = sensorId;
        this.timestampMs = timestampMs;
        this.isGarbage = isGarbage;
    }

    public abstract SensorType getSensorType();

    /** Getters for common metadata fields. */
    public SensorId getSensorId() { return sensorId; }
    public long getTimestampMs() { return timestampMs; }
    public boolean isGarbage() { return isGarbage; }
}