package com.team30.core.logic;

import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.BrakeResult;

/**
 *
 * Follows a template method pattern
 */
public abstract class AEBSPipline {
    public final void processData(SensorData data){
        // Step 1: Check for redundancy in sensor data
        if (redundancyChecker(data)){
            faultHandler(data);
            return;
        }

        // Step 2: Perform collision detection
        if (!collisionDetection(data)) return;

        // Step 3: Control the braking system based on the processed data
        // Todo: not sure of how to do retries
        BrakeResult result = brakingSystemController(data);
        if(result == BrakeResult.FAILED || result == BrakeResult.EXHAUSTED){
            faultHandler(data);
        }
    }

    /**
     * Checks for redundancy in the processed sensor data to ensure reliability and consistency.
     * @param data
     * @return true if redundancy issues are detected, false otherwise
     */
    protected abstract boolean redundancyChecker(ProcessedSensorData data);

    /**
     * Performs collision detection using the processed sensor data
     * to determine if there is an imminent collision threat.
     * @param data
     * @return true if a collision threat is detected, false otherwise
     */
    protected abstract boolean collisionDetection(ProcessedSensorData data);

    /**
     * Controls the braking system based on the processed sensor data
     * and the results of collision detection.
     * @param data
     * @return the result of the braking action, indicating success, failure, exhaustion, clearance, or if braking was not needed
     */
    protected abstract BrakeResult brakingSystemController(ProcessedSensorData data);

    /**
     * Handles faults detected during the processing of sensor data
     * Signals to the driver
     * @param data
     */
    protected abstract void faultHandler(ProcessedSensorData data);
}
