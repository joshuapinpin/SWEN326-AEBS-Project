package com.team30.core.datalayer.observers;

import com.team30.core.datalayer.data.SensorData;

/**
 * Observer interface for receiving updates from sensors.
 * Will be implemented by the AEBSSoftwareSystem
 */
public interface SensorObserver {
    /**
     * Called when new sensor data is available.
     * The AEBSSoftwareSystem will implement this method to receive updates from the sensors
     * and update the sensor input handler buffer.
     * @param data The new sensor data to process.
     */
    void update(SensorData data);
}
