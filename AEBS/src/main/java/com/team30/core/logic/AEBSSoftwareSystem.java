package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.ThreatLevel;
import com.team30.core.datalayer.observers.SensorObserver;
import com.team30.core.presentation.DriverInterface;

/**
 * The AEBSSoftwareSystem class implements the logical flow of the Autonomous Emergency Braking System (AEBS)
 * by extending the AEBSPipeline and implementing the SensorObserver interface.
 */
public class AEBSSoftwareSystem extends AEBSPipeline implements SensorObserver {
    private final SensorInputHandler sensorInputHandler;
    private final RedundancyChecker redundancyChecker;
    private final CollisionDetector collisionDetector;
    private final BrakeSystemController brakeSystemController;
    private final FaultHandler faultHandler;
    private final DriverInterface driverInterface;

    private ThreatLevel previousThreat = ThreatLevel.NONE;
    private BrakeDecision latestBrakeDecision;

    /**
     * Constructor for the AEBSSoftwareSystem class, initializing all components of the system.
     * @param sensorInputHandler the handler responsible for managing sensor data input and buffering
     * @param redundancyChecker the component responsible for checking redundancy in sensor data to ensure reliability
     * @param collisionDetector the component responsible for performing collision detection based on processed sensor data
     * @param brakeSystemController the component responsible for controlling the braking system based on collision assessments
     * @param faultHandler the component responsible for handling faults detected during the pipeline execution
     * @param driverInterface the interface responsible for communicating with the driver, providing alerts and feedback based on system status
     */
    public AEBSSoftwareSystem(SensorInputHandler sensorInputHandler,
                              RedundancyChecker redundancyChecker,
                              CollisionDetector collisionDetector,
                              BrakeSystemController brakeSystemController,
                              FaultHandler faultHandler,
                              DriverInterface driverInterface) {
        this.sensorInputHandler = sensorInputHandler;
        this.redundancyChecker = redundancyChecker;
        this.collisionDetector = collisionDetector;
        this.brakeSystemController = brakeSystemController;
        this.faultHandler = faultHandler;
        this.driverInterface = driverInterface;
    }

    @Override
    public void update(SensorData data) {
        sensorInputHandler.addToBuffer(data);
    }

    @Override
    protected ProcessedSensorData retrieveLatestSensorSnapshot() {
        return sensorInputHandler.getLatest();
    }

    @Override
    protected ProcessedSensorData redundancyChecker(ProcessedSensorData data) {
        return redundancyChecker.validate(data);
    }

    @Override
    protected CollisionAssessment collisionDetection(ProcessedSensorData data) {
        CollisionAssessment assessment = collisionDetector.assess(data);
        if (assessment == null) {
            return null;
        }
        ThreatLevel current = assessment.getThreatLevel();

        // Keep BRAKE latched while braking
        if (previousThreat == ThreatLevel.BRAKE && current != ThreatLevel.BRAKE) {
            current = ThreatLevel.BRAKE;
        }

        // Only react when threat level changes
        if (current != previousThreat) {
            switch (current) {
                case WARNING -> {
                    driverInterface.emitAuditoryAlert();
                    driverInterface.showVisualAlert();
                }
                case BRAKE -> {
                    if (previousThreat == ThreatLevel.NONE) {
                        driverInterface.emitAuditoryAlert();
                        driverInterface.showVisualAlert();
                    }
                    driverInterface.showBrakingActivated();
                }
                case NONE -> {
                }
            }
            previousThreat = current;
        }
        return assessment;
    }

    @Override
    protected BrakeDecision brakingSystemController(CollisionAssessment assessment) {
        latestBrakeDecision = brakeSystemController.execute(assessment);

        if (latestBrakeDecision.getResult() == BrakeResult.FAILED) {
            driverInterface.showBrakingActivated();
        }

        return latestBrakeDecision;
    }

    @Override
    protected void faultHandler(BrakeDecision decision, ProcessedSensorData validatedData) {
        faultHandler.handle(decision, validatedData);
    }

    /** Getters and Setters */

    public BrakeDecision getLatestBrakeDecision() {
        return latestBrakeDecision;
    }
    public ThreatLevel getPreviousThreat() {
        return previousThreat;
    }

    public void setBrakeFailures(int count) {
        brakeSystemController.setBrakeFailures(count);
    }

    public boolean hasCriticalFailure() {
        return faultHandler.hasCriticalFailure();
    }
    public void showMaintenanceWarning(SensorType type) {
        driverInterface.showMaintenanceWarning(type);
    }
}
