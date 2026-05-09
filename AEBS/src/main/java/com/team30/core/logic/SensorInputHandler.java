package com.team30.core.logic;

import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.observers.SensorObserver;

import java.util.*;

public class SensorInputHandler implements SensorObserver {

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
     */
    @Override
    public void update(SensorData data) {
        if (data == null) return;
        buffer.get(data.getSensorType()).put(data.getSensorId(), data);
        updatedSensorTypes.add(data.getSensorType());
    }

    /**
     * Called by AEBSSoftwareSystem to pull a snapshot of all latest readings.
     * Returns null if no data has been buffered yet.
     */
    public ProcessedSensorData getLatest() {
        if (isBufferEmpty()) return null;
        ProcessedSensorData processed = buildProcessedData();
        updatedSensorTypes.clear();
        return processed;
    }

    /**
     * Returns true if radar or lidar data arrived since the last getLatest() call.
     */
    public boolean hasNewRadarOrLidar() {
        return updatedSensorTypes.contains(SensorType.RADAR) || updatedSensorTypes.contains(SensorType.LIDAR);
    }

    private boolean isBufferEmpty() {
        for (SensorType type : SensorType.values()) {
            if (!buffer.get(type).isEmpty()) return false;
        }
        return true;
    }

    private ProcessedSensorData buildProcessedData() {
        Map<SensorType, Map<SensorId, SensorData>> snapshot = new HashMap<>();
        for (SensorType type : SensorType.values()) {
            snapshot.put(type, new HashMap<>(buffer.get(type)));
        }
        return new ProcessedSensorData(snapshot, System.currentTimeMillis());
    }
}