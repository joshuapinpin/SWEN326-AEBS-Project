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

public class AEBSSoftwareSystem extends AEBSPipeline implements SensorObserver {
    private SensorInputHandler sensorInputHandler;
    private RedundancyChecker redundancyChecker;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeSystemController;
    private FaultHandler faultHandler;
    private DriverInterface driverInterface;

    private ThreatLevel previousThreat = ThreatLevel.NONE;

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
        sensorInputHandler.update(data);
    }

    @Override
    protected ProcessedSensorData handleSensorInput() {
        return sensorInputHandler.getLatest();
    }

    @Override
    protected ProcessedSensorData redundancyChecker(ProcessedSensorData data) {
        return redundancyChecker.validate(data);
    }



    private BrakeDecision latestBrakeDecision;

    @Override
    protected CollisionAssessment collisionDetection(ProcessedSensorData data) {
        CollisionAssessment assessment =
                collisionDetector.assess(data);
        if (assessment == null) {
            return null;
        }
        ThreatLevel current = assessment.getThreatLevel();

// Keep BRAKE latched while braking
        if (previousThreat == ThreatLevel.BRAKE
                && current != ThreatLevel.BRAKE) {
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

    public BrakeDecision getLatestBrakeDecision() {
        return latestBrakeDecision;
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
