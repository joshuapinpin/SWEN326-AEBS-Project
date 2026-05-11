package com.team30.core.datalayer.observers;

import com.team30.core.datalayer.data.SensorData;

/**
 * Subject interface for managing sensor observers.
 * Sensors will implement this interface to allow the AEBSSoftwareSystem to register as an observer and receive updates.
 */
public interface SensorSubject {
    /**
     * Registers an observer to receive updates from the sensor.
     * @param observer The observer to register.
     */
    void attachObserver(SensorObserver observer);

    /**
     * Removes an observer from receiving updates from the sensor.
     * @param observer The observer to remove.
     */
    void detachObserver(SensorObserver observer);

    /**
     * Notifies all registered observers of new sensor data.
     * This method will be called by the sensor when new data is available,
     * and it will trigger the update method in all registered observers (like the AEBSSoftwareSystem)
     * to process the new data.
     * @param data The new sensor data to notify observers about.
     */
    void notifyObservers(SensorData data);
}
