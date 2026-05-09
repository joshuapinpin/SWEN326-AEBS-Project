package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;

/**
 *
 * Follows a template method pattern
 */
public abstract class AEBSPipeline {
    public final void runPipeline(){
        // Step 1: Get the latest snapshot
        ProcessedSensorData processedData = handleSensorInput();
        if(processedData == null) return; // the buffer was empty; nothing to do

        // Step 2: Check for redundancy in sensor data and validate it
        ProcessedSensorData validatedData = redundancyChecker(processedData);

        // Step 3: Perform collision detection using the validated sensor data
        CollisionAssessment assessment = collisionDetection(validatedData);

        // Step 4: Control the braking system based on the collision assessment
        BrakeDecision decision = brakingSystemController(assessment);

        // Step 5: Handle any faults detected during the pipeline execution
        faultHandler(decision, validatedData);
    }

    /**
     * Gets the latest snapshot of sensor data from the SensorInputHandler buffer.
     * Todo: change method name to retrieveLatestSnapshot
     */
    protected abstract ProcessedSensorData handleSensorInput();

    /**
     * Checks for redundancy in the processed sensor data to ensure reliability and consistency.
     *
     * @param data
     * @return true if redundancy issues are detected, false otherwise
     */
    protected abstract ProcessedSensorData redundancyChecker(ProcessedSensorData data);

    /**
     * Performs collision detection using the processed sensor data
     * to determine if there is an imminent collision threat.
     *
     * @param data
     * @return true if a collision threat is detected, false otherwise
     */
    protected abstract CollisionAssessment collisionDetection(ProcessedSensorData data);

    /**
     * Controls the braking system based on the processed sensor assessment
     * and the results of collision detection.
     *
     * @param assessment
     * @return the result of the braking action, indicating success, failure, exhaustion, clearance, or if braking was not needed
     */
    protected abstract BrakeDecision brakingSystemController(CollisionAssessment assessment);

    /**
     * Handles faults detected during the processing of sensor decision
     * Signals to the driver
     * @param decision
     */
    protected abstract void faultHandler(BrakeDecision decision, ProcessedSensorData validatedData);
}
