package com.team30.core.logic;

import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.observers.SensorObserver;

public class AEBSSoftwareSystem extends AEBSPipline implements SensorObserver {

    @Override
    public void update(SensorData data) {
        this.processData(data);
    }

    @Override
    protected boolean redundancyChecker(ProcessedSensorData data) {
        return false;
    }

    @Override
    protected boolean collisionDetection(ProcessedSensorData data) {
        return false;
    }

    @Override
    protected BrakeResult brakingSystemController(ProcessedSensorData data) {
        return null;
    }

    @Override
    protected void faultHandler(ProcessedSensorData data) {

    }
}
