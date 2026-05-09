package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import java.util.Map;

public class FaultHandler {

    private final DriverInterface driverInterface;
    private final CarState carState;

    public FaultHandler(DriverInterface driverInterface, CarState carState) {
        this.driverInterface = driverInterface;
        this.carState = carState;
    }

    /**
     * Handles faults based on brake decision result and sensor availability.
     * - EXHAUSTED braking → escalation alert
     * - Multiple sensor types unavailable → fail-safe + escalation alert
     * - Single sensor type unavailable → maintenance warning
     *
     * @param decision the BrakeDecision from BrakeSystemController
     * @param data     the validated ProcessedSensorData from RedundancyChecker
     */
    public void handle(BrakeDecision decision, ProcessedSensorData data) {
        if (decision.getResult() == BrakeResult.EXHAUSTED) {
            engageFailSafe();
            driverInterface.showEscalationAlert();
            return;
        }

        int unavailableCount = 0;
        for (SensorType type : SensorType.values()) {
            if (isSensorTypeUnavailable(data, type)) {
                unavailableCount++;
            }
        }

        if (unavailableCount >= 2) {
            engageFailSafe();
            driverInterface.showEscalationAlert();
        } else if (unavailableCount == 1) {
            driverInterface.showMaintenanceWarning();
        }
    }

    /**
     * Engages fail-safe mode — sets DrivingMode to FAIL_SAFE,
     * brings target speed to zero, applies maximum deceleration.
     */
    private void engageFailSafe() {
        carState.setDrivingMode(DrivingMode.FAIL_SAFE);
        carState.setTargetSpeed(0.0);
        carState.setDecelerationRate(8.0);
    }

    /**
     * Returns true if both PRIMARY and REDUNDANT for the given SensorType
     * are absent from the validated snapshot — meaning RedundancyChecker
     * stripped both as garbage.
     */
    private boolean isSensorTypeUnavailable(ProcessedSensorData data, SensorType type) {
        Map<SensorId, SensorData> typeReadings = data.getReadings().get(type);
        return typeReadings == null || typeReadings.isEmpty();
    }
}