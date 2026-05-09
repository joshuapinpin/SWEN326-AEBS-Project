package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

public class LidarData extends SensorData {
    private final double distance;       // metres ahead, -1 if nothing detected
    private final double relativeSpeed;  // m/s, positive = closing
    private final boolean objectDetected;

    public LidarData(SensorId sensorId, long timestampMs,
                     double distance, double relativeSpeed, boolean objectDetected) {
        super(sensorId, timestampMs, false);
        this.distance = distance;
        this.relativeSpeed = relativeSpeed;
        this.objectDetected = objectDetected;
    }

    /** Garbage constructor */
    public LidarData(SensorId sensorId, long timestampMs) {
        super(sensorId, timestampMs, true);
        this.distance = 9999.0;
        this.relativeSpeed = 9999.0;
        this.objectDetected = false;
    }

    public double getDistance()       { return distance; }
    public double getRelativeSpeed()  { return relativeSpeed; }
    public boolean isObjectDetected() { return objectDetected; }

    @Override
    public SensorType getSensorType() { return SensorType.LIDAR; }
}