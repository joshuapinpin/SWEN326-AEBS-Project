package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.observers.SensorObserver;
import com.team30.core.presentation.DriverInterface;

public class AEBSSoftwareSystem extends AEBSPipeline implements SensorObserver {
    private SensorInputHandler sensorInputHandler;
    private RedundancyChecker redundancyChecker;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeSystemController;
    private FaultHandler faultHandler;
    private DriverInterface driverInterface;

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
        CollisionAssessment assessment = collisionDetector.assess(data);

        if (assessment == null) return null;

        // Alert driver based on threat level
        switch (assessment.getThreatLevel()) {
            case WARNING -> {
                driverInterface.emitAuditoryAlert();
                driverInterface.showVisualAlert();
            }
            case BRAKE -> {
                driverInterface.showBrakingActivated();
            }
            case NONE -> {
                // no alert needed
            }
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
