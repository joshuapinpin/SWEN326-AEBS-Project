package com.team30.core.logic;

import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

import java.util.*;

/**
 * The SensorInputHandler class is responsible for managing the buffering of sensor data received
 * from various sensors in the Autonomous Emergency Braking System (AEBS),
 * and converting them into a format suitable for processing by the AEBSSoftwareSystem.
 */
public class SensorInputHandler{

    private final Map<SensorType, Map<SensorId, SensorData>> buffer;
    private final Set<SensorType> updatedSensorTypes;

    public SensorInputHandler() {
        this.buffer = new HashMap<>();
        this.updatedSensorTypes = new HashSet<>();

        for (SensorType type : SensorType.values()) {
            buffer.put(type, new HashMap<>());
        }
    }

    /**
     * Called by each sensor via the observer pattern whenever it fires.
     * Buffers the latest reading keyed by type and ID.
     * @param data the sensor data to be added to the buffer
     */
    public void addToBuffer(SensorData data) {
        if (data == null) return;
        buffer.get(data.getSensorType()).put(data.getSensorId(), data);
        updatedSensorTypes.add(data.getSensorType());
    }

    /**
     * Called by AEBSSoftwareSystem to pull a snapshot of all latest readings.
     * @return a ProcessedSensorData object containing the latest sensor data, or null if the buffer is empty
     */
    public ProcessedSensorData getLatest() {
        if (isBufferEmpty()) return null;
        ProcessedSensorData processed = buildProcessedData();
        updatedSensorTypes.clear();
        return processed;
    }

    /**
     * Returns true if radar or lidar data arrived since the last getLatest() call.
     * @return true if new radar or lidar data is available, false otherwise
     */
    public boolean hasNewRadarOrLidar() {
        return updatedSensorTypes.contains(SensorType.RADAR) || updatedSensorTypes.contains(SensorType.LIDAR);
    }

    /**
     * Checks if the buffer is empty, meaning no sensor data has been received since the last snapshot was taken.
     * @return true if the buffer is empty, false otherwise
     */
    private boolean isBufferEmpty() {
        for (SensorType type : SensorType.values()) {
            if (!buffer.get(type).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Builds a ProcessedSensorData object containing a snapshot of the latest sensor data from the buffer.
     * @return a ProcessedSensorData object with the latest sensor data and a timestamp
     */
    private ProcessedSensorData buildProcessedData() {
        Map<SensorType, Map<SensorId, SensorData>> snapshot = new HashMap<>();
        for (SensorType type : SensorType.values()) {
            snapshot.put(type, new HashMap<>(buffer.get(type)));
        }
        return new ProcessedSensorData(snapshot, System.currentTimeMillis());
    }
}