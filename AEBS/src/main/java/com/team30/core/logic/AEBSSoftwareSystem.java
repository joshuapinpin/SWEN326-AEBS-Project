package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
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



    @Override
    protected CollisionAssessment collisionDetection(ProcessedSensorData data) {
        CollisionAssessment assessment =
                collisionDetector.assess(data);
        if (assessment == null) {
            return null;
        }
        ThreatLevel current =
                assessment.getThreatLevel();
        // Only react when threat level changes
        if (current != previousThreat) {

            switch (current) {
                case WARNING -> {
                    driverInterface.emitAuditoryAlert();
                    driverInterface.showVisualAlert();
                }

                case BRAKE -> {
                    // Optional:
                    // still warn before braking
                    if (previousThreat == ThreatLevel.NONE) {
                        driverInterface.emitAuditoryAlert();
                        driverInterface.showVisualAlert();
                    }
                    driverInterface.showBrakingActivated();
                }
                case NONE -> {
                    // threat cleared
                }
            }
            previousThreat = current;
        }
        return assessment;
    }

    @Override
    protected BrakeDecision brakingSystemController(CollisionAssessment assessment) {
        return brakeSystemController.execute(assessment);
    }

    @Override
    protected void faultHandler(BrakeDecision decision, ProcessedSensorData validatedData) {
        faultHandler.handle(decision, validatedData);
    }
}
