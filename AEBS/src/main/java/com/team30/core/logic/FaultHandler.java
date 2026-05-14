package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import java.util.Map;

/**
 * FaultHandler is responsible for monitoring the health of the braking system
 * and sensor availability. It reacts to critical conditions by escalating alerts
 * to the driver and setting a critical failure state that can be checked by
 * other components.
 *
 * Fault conditions include:
 * - EXHAUSTED brake decision result from BrakeSystemController
 * - Unavailability of sensor types (both PRIMARY and REDUNDANT stripped)
 *
 * The class ensures that escalation alerts are shown only once per critical failure event.
 */
public class FaultHandler {

    private final DriverInterface driverInterface;
    private boolean escalationAlertShown = false;
    private boolean criticalFailure = false;

    public FaultHandler(DriverInterface driverInterface, CarState carState) {
        this.driverInterface = driverInterface;
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
        assert decision != null : "BrakeDecision must not be null";
        assert data != null : "ProcessedSensorData must not be null";
        if (decision.getResult() == BrakeResult.EXHAUSTED) {
            escalateCriticalFailure();
            return;
        }

        int unavailableCount = 0;
        // Unused variable
        // SensorType unavailableType = null;

        for (SensorType type : SensorType.values()) {
            if (isSensorTypeUnavailable(data, type)) {
                unavailableCount++;
                // unavailableType = type;
            }
        }

        if (unavailableCount >= 1) {
            escalateCriticalFailure();

        } else {
            escalationAlertShown = false;
        }
    }

    public boolean hasCriticalFailure() {
        return criticalFailure;
    }

    private void escalateCriticalFailure() {
        assert !criticalFailure : "Critical failure should only be set once per escalation event";
        criticalFailure = true;

        if (!escalationAlertShown) {
            assert driverInterface != null : "DriverInterface must not be null";
            driverInterface.showEscalationAlert();
            escalationAlertShown = true;
        }
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