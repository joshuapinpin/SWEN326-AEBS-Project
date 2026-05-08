package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

/**
 * Class representing sensor data in the AEBS system.
 * Includes the sensor type, sensor ID (primary or redundant), the value of the reading, and the time the reading was taken.
 * SensorReading holds the raw sensor data which is then passed to the SensorInputHandler for validation and formatting.
 * @author Hayley Far
 */
public class SensorData {
    private final SensorType sensorType;
    private final SensorId sensorId;
    private final double value;
    private final long timestamp;

    /**
     * Constructor for SensorData.
     * @param sensorType The type of sensor (RADAR, LIDAR, CAMERA, WHEEL_SPEED).
     * @param sensorId The identifier for the sensor (PRIMARY or REDUNDANT).
     * @param value The value of the sensor reading.
     * @param timestamp The time the reading was taken (in milliseconds since epoch).
     */
    public SensorData(SensorType sensorType, SensorId sensorId, double value, long timestamp) {
        this.sensorType = sensorType;
        this.sensorId = sensorId;
        this.value = value;
        this.timestamp = timestamp;
    }

    /**
     * Returns the type of sensor that produced this reading.
     * @return The sensor type.
     */
    public SensorType getSensorType() { return sensorType; }

    /**
     * Returns the identifier of the sensor that produced this reading (primary or redundant).
     * @return The sensor ID.
     */
    public SensorId getSensorId() { return sensorId; }

    /**
     * Returns the value of the sensor reading.
     * @return The value of the reading.
     */
    public double getValue() { return value; }

    /**
     * Returns the timestamp of the sensor reading.
     * @return The timestamp in milliseconds since epoch.
     */
    public long getTimestamp() { return timestamp; }

    /**
     * Returns a string representation of the sensor reading for debugging.
     * @return A string representation of the sensor reading.
     */
    @Override
    public String toString() {
        return "SensorReading{" +
                "sensorType=" + sensorType +
                ", sensorId=" + sensorId +
                ", value=" + value +
                ", timestamp=" + timestamp +
                '}';
    }
}
