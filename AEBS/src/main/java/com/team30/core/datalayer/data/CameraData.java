package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.ObjectType;
import com.team30.core.datalayer.enums.SensorId;

public class CameraData extends SensorData {
    private final ObjectType classification;  // UNKNOWN if below confidence threshold
    private final boolean inCurrentLane;
    private final double confidence;          // 0.0 to 1.0

    public CameraData(SensorId sensorId, long timestampMs,
                      ObjectType classification, boolean inCurrentLane, double confidence) {
        super(sensorId, timestampMs, false);
        this.classification = classification;
        this.inCurrentLane = inCurrentLane;
        this.confidence = confidence;
    }

    /** Garbage constructor */
    public CameraData(SensorId sensorId, long timestampMs) {
        super(sensorId, timestampMs, true);
        this.classification = ObjectType.UNKNOWN;
        this.inCurrentLane = false;
        this.confidence = 0.0;
    }

    public ObjectType getClassification() { return classification; }
    public boolean isInCurrentLane()      { return inCurrentLane; }
    public double getConfidence()         { return confidence; }
}