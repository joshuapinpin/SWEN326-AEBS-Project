package com.team30.core.logic;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

import java.util.*;

/**
 * Handles incoming raw sensor data in the AEBS system.
 * Buffers the latest SensorData for each sensor type and ID.
 * Tracks which sensor types have received new updated data in the current cycle.
 * Produces a ProcessedSensorData object from the current buffer when requested.
 *
 * Acts as the entry point for all sensor data into the AEBS software system.
 * Called by AEBSSoftwareSystem as the first step in the pipeline.
 */
public class SensorInputHandler {
    /**
     * Buffer storing the latest SensorData for each sensor type and ID.
     * Outer key: SensorType (RADAR, LIDAR, CAMERA, WHEEL_SPEED)
     * Inner key: SensorId (PRIMARY, REDUNDANT)
     * New data for the same type and ID overwrites the previous reading.
     */
    private final Map<SensorType, Map<SensorId, SensorData>> buffer;

    /**
     * Tracks which sensor types have received new data in the current cycle.
     * Cleared after getLatest() is called to reset for the next cycle.
     */
    private final Set<SensorType> updatedSensorTypes;

    /**
     * Constructs a SensorInputHandler with an empty buffer and new types set.
     * Initializes the buffer with empty maps for each SensorType to avoid null checks later.
     */
    public SensorInputHandler() {
        this.buffer = new HashMap<>();
        this.updatedSensorTypes = new HashSet<>();

        for (SensorType type : SensorType.values()) {
            buffer.put(type, new HashMap<>());
        }
    }

    /**
     * Receives a SensorData object, stores it in the buffer keyed by
     * sensor type and ID, and marks the sensor type as updated this cycle.
     * New data for the same type and ID overwrites the previous reading,
     * ensuring the buffer always holds the latest value.
     * @param data the incoming SensorData to buffer
     */
    public void update(SensorData data) {
        if (data == null) {return;}
        buffer.get(data.getSensorType()).put(data.getSensorId(), data);
        updatedSensorTypes.add(data.getSensorType());
    }

    /**
     * Returns a ProcessedSensorData object built from the current buffer.
     * Takes a snapshot of the buffer to prevent downstream modification.
     * Clears updatedSensorTypes after building to reset for the next cycle.
     * Returns null if the buffer has no readings for any sensor type.
     * @return ProcessedSensorData from current buffer, or null if buffer is empty
     */
    public ProcessedSensorData getLatest() {
        if (isBufferEmpty()) {return null;}

        ProcessedSensorData processed = buildProcessedData();
        updatedSensorTypes.clear();
        return processed;
    }

    /**
     * Returns true if updated radar or lidar data has been received this cycle.
     * Used by CollisionDetector to determine if a new assessment is needed.
     * @return true if RADAR or LIDAR type is in updatedSensorTypes
     */
    public boolean hasNewRadarOrLidar() {
        return updatedSensorTypes.contains(SensorType.RADAR) || updatedSensorTypes.contains(SensorType.LIDAR);
    }

    /**
     * Returns true if the buffer has no readings for any sensor type.
     * @return true if all inner maps are empty
     */
    private boolean isBufferEmpty() {
        for (SensorType type : SensorType.values()) {
            if (!buffer.get(type).isEmpty()) {return false;}
        }
        return true;
    }

    /**
     * Builds a ProcessedSensorData object from a snapshot of the current buffer.
     * Snapshot prevents the buffer being modified after ProcessedSensorData is built.
     *
     * @return ProcessedSensorData containing a snapshot of all current buffered readings
     */
    private ProcessedSensorData buildProcessedData() {
        Map<SensorType, Map<SensorId, SensorData>> snapshot = new HashMap<>();

        for (SensorType type : SensorType.values()) {
            snapshot.put(type, new HashMap<>());
        }
        return new ProcessedSensorData(snapshot, System.currentTimeMillis());
    }
}