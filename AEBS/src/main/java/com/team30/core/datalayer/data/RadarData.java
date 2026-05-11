package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

/**
 * Represents a single reading from a radar sensor in the AEBS system.
 * Includes the distance to the object ahead, the relative speed of that object,
 * and whether an object is detected at all.
 */
public class RadarData extends SensorData {
    private final double distance;       // metres ahead, -1 if nothing detected
    private final double relativeSpeed;  // m/s, positive = closing
    private final boolean objectDetected;

    /**
     * Constructs a RadarData object with the given parameters.
     * @param sensorId the ID of the radar sensor (PRIMARY or REDUNDANT)
     * @param timestampMs the timestamp of this reading in milliseconds since epoch
     * @param distance the distance to the object ahead in meters, or -1 if no object detected
     * @param relativeSpeed the relative speed of the object in m/s (positive means closing), or -1 if no object detected
     * @param objectDetected true if an object is detected, false if no object detected
     */
    public RadarData(SensorId sensorId, long timestampMs, double distance, double relativeSpeed, boolean objectDetected) {
        super(sensorId, timestampMs, false);
        this.distance = distance;
        this.relativeSpeed = relativeSpeed;
        this.objectDetected = objectDetected;
    }

    /** Garbage constructor */
    public RadarData(SensorId sensorId, long timestampMs) {
        super(sensorId, timestampMs, true);
        this.distance = 9999.0;
        this.relativeSpeed = 9999.0;
        this.objectDetected = false;
    }

    /** Getters for radar-specific data fields. */
    public double getDistance() { return distance; }
    public double getRelativeSpeed() { return relativeSpeed; }
    public boolean isObjectDetected() { return objectDetected; }

    @Override
    public SensorType getSensorType() { return SensorType.RADAR; }
}