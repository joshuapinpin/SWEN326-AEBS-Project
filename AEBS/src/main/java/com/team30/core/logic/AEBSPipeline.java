package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;

/**
 * The AEBSPipeline class defines the structure of the pipeline for processing sensor data, performing redundancy checks,
 * collision detection, and controlling the braking system in an Autonomous Emergency Braking System (AEBS).
 * It follows the Template Method design pattern, allowing subclasses to implement specific steps of the pipeline
 * while maintaining a consistent overall flow.
 */
public abstract class AEBSPipeline {

    /**
     * The template method that runs the entire pipeline for processing sensor data, performing redundancy checks,
     * collision detection, and controlling the braking system.
     */
    public final void runPipeline(){
        // Step 1: Get the latest snapshot
        ProcessedSensorData processedData = retrieveLatestSensorSnapshot();
        if(processedData == null) return; // the buffer was empty; nothing to do

        // Step 2: Check for redundancy in sensor data and validate it
        ProcessedSensorData validatedData = redundancyChecker(processedData);

        // Step 3: Perform collision detection using the validated sensor data
        CollisionAssessment assessment = collisionDetection(validatedData);
        if (assessment == null) return;

        // Step 4: Control the braking system based on the collision assessment
        BrakeDecision decision = brakingSystemController(assessment);

        // Step 5: Handle any faults detected during the pipeline execution
        faultHandler(decision, validatedData);
    }

    /**
     * Gets the latest snapshot of sensor data from the SensorInputHandler buffer.
     * @return the latest processed sensor data, or null if the buffer is empty
     */
    protected abstract ProcessedSensorData retrieveLatestSensorSnapshot();

    /**
     * Checks for redundancy in the processed sensor data to ensure reliability and consistency.
     *
     * @param data the processed sensor data to be checked for redundancy
     * @return true if redundancy issues are detected, false otherwise
     */
    protected abstract ProcessedSensorData redundancyChecker(ProcessedSensorData data);

    /**
     * Performs collision detection using the processed sensor data
     * to determine if there is an imminent collision threat.
     *
     * @param data the processed sensor data to be analyzed for collision detection
     * @return true if a collision threat is detected, false otherwise
     */
    protected abstract CollisionAssessment collisionDetection(ProcessedSensorData data);

    /**
     * Controls the braking system based on the processed sensor assessment
     * and the results of collision detection.
     *
     * @param assessment the collision assessment containing information about the detected collision threat
     * @return the result of the braking action, indicating success, failure, exhaustion, clearance, or if braking was not needed
     */
    protected abstract BrakeDecision brakingSystemController(CollisionAssessment assessment);

    /**
     * Handles faults detected during the processing of sensor decision
     * Signals to the driver
     * @param decision the result of the braking action, indicating success, failure, exhaustion, clearance, or if braking was not needed
     * @param validatedData the processed sensor data that was validated during the redundancy check
     */
    protected abstract void faultHandler(BrakeDecision decision, ProcessedSensorData validatedData);
}
