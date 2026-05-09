package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

import java.util.Map;

/**
 * Represents a snapshot of the most recent sensor readings from all sensor
 * types at a given point in time. Produced by SensorInputHandler and passed
 * through the AEBS pipeline to RedundancyChecker and CollisionDetector.
 */
public class ProcessedSensorData {
    /**
     * Map storing the latest SensorData for each sensor type and ID.
     * Outer key: SensorType (RADAR, LIDAR, CAMERA, WHEEL_SPEED)
     * Inner key: SensorId (PRIMARY, REDUNDANT)
     */
    private final Map<SensorType, Map<SensorId, SensorData>> readings;

    /**
     * Timestamp of when this ProcessedSensorData was produced
     * in milliseconds since epoch.
     */
    private final long timestamp;

    /**
     * Constructs a ProcessedSensorData with the given readings snapshot
     * and timestamp.
     * @param readings  snapshot of all current buffered sensor readings
     * @param timestamp time this snapshot was produced in milliseconds since epoch
     */
    public ProcessedSensorData(Map<SensorType, Map<SensorId, SensorData>> readings, long timestamp) {
        this.readings = readings;
        this.timestamp = timestamp;
    }

    /**
     * Returns the SensorData for the given sensor type and ID.
     * Returns null if no reading is available for that combination.
     * @param type the SensorType to retrieve
     * @param id   the SensorId to retrieve (PRIMARY or REDUNDANT)
     * @return SensorData for that type and ID, or null if unavailable
     */
    public SensorData getSensorData(SensorType type, SensorId id) {
        Map<SensorId, SensorData> typeReadings = readings.get(type);
        if (typeReadings == null) {
            return null;
        }
        return typeReadings.get(id);
    }

    /**
     * Returns true if a reading is available for the given sensor type and ID.
     * Used by RedundancyChecker to determine which sensors are available
     * before attempting cross-checking.
     * @param type the SensorType to check
     * @param id   the SensorId to check
     * @return true if a reading exists for that type and ID
     */
    public boolean isSensorAvailable(SensorType type, SensorId id) {
        return getSensorData(type, id) != null;
    }

    /**
     * Returns true if updated radar or lidar data is present in this snapshot.
     * Used by CollisionDetector to determine if a new assessment is needed.
     * Checks directly against the readings map.
     * @return true if RADAR or LIDAR readings are present in this snapshot
     */
    public boolean hasNewRadarOrLidar() {
        boolean hasRadar = readings.containsKey(SensorType.RADAR) && !readings.get(SensorType.RADAR).isEmpty();
        boolean hasLidar = readings.containsKey(SensorType.LIDAR) && !readings.get(SensorType.LIDAR).isEmpty();
        return hasRadar || hasLidar;
    }

    /**
     * Returns the timestamp of when this snapshot was produced.
     * @return timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the full readings map for all sensor types and IDs.
     * @return map of all sensor readings
     */
    public Map<SensorType, Map<SensorId, SensorData>> getReadings() {
        return readings;
    }

    /**
     * Returns a string representation of this snapshot for debugging.
     * @return string representation of all sensor readings and timestamp
     */
    @Override
    public String toString() {
        return "ProcessedSensorData{" +
                "readings=" + readings +
                ", timestamp=" + timestamp +
                '}';
    }
}