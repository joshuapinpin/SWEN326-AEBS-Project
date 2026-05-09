package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.observers.SensorObserver;

public class AEBSSoftwareSystem extends AEBSPipeline implements SensorObserver {
    private SensorInputHandler sensorInputHandler;
    private RedundancyChecker redundancyChecker;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeSystemController;
    private FaultHandler faultHandler;

    public AEBSSoftwareSystem(SensorInputHandler sensorInputHandler,
                              RedundancyChecker redundancyChecker,
                              CollisionDetector collisionDetector,
                              BrakeSystemController brakeSystemController,
                              FaultHandler faultHandler) {
        this.sensorInputHandler = sensorInputHandler;
        this.redundancyChecker = redundancyChecker;
        this.collisionDetector = collisionDetector;
        this.brakeSystemController = brakeSystemController;
        this.faultHandler = faultHandler;
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
        return collisionDetector.assess(data);
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
