package com.team30.core.presentation;

import com.team30.core.datalayer.enums.SensorType;
import com.team30.simulation.state.CarState;

/**
 * Presentation layer component responsible for communicating AEBS system
 * state to the driver. Reads from CarState to display current vehicle
 * information and outputs auditory and visual alerts when
 * hazards are detected, braking is activated, retries are exhausted,
 * or sensor faults occur.
*/
public class DriverInterface {
    private final CarState carState;
    private boolean aesbActive;

    /**
     * Constructs a DriverInterface with the given CarState.
     * AEBS is active by default on system startup.
     * @param carState the CarState to read vehicle information from
     */
    public DriverInterface(CarState carState) {
        this.carState = carState;
        this.aesbActive = true;
    }

    /**
     * Emits an auditory alert when a hazard is detected within
     * dangerous proximity of the vehicle.
     * Displays current vehicle speed and driving mode from CarState.
     */
    public void emitAuditoryAlert() {
        System.out.println("\n[AUDITORY ALERT] *** BEEP BEEP BEEP ***");
        System.out.println("[AUDITORY ALERT] Hazard detected in proximity!");
        printCarState();
    }

    /**
     * Displays a visual alert to the driver when a hazard is detected.
     */
    public void showVisualAlert() {
        System.out.println("\n[VISUAL ALERT] *** WARNING ***");
        System.out.println("[VISUAL ALERT] Hazard detected — AEBS monitoring.");
        printCarState();
    }

    /**
     * Displays a visual alert to the driver when automatic emergency
     * braking has been activated by the AEBS system.
     */
    public void showBrakingActivated() {
        System.out.println("\n[VISUAL ALERT] *** AUTOMATIC BRAKING ACTIVATED ***");
        System.out.println("[VISUAL ALERT] AEBS is applying brakes.");
        printCarState();
    }

    /**
     * Toggles the AEBS system on or off based on driver input.
     * User can manually deactivate AEBS if they choose.
     * When deactivated the system will not respond to hazards.
     * @param active true to activate AEBS, false to deactivate
     */
    public void toggleAEBS(boolean active) {
        this.aesbActive = active;
        if (active) {
            System.out.println("\n[AEBS] System ACTIVATED by driver.");
        } else {
            System.out.println("\n[AEBS] System DEACTIVATED by driver.");
        }
        System.out.println("[AEBS] Current status: "
                + (aesbActive ? "ACTIVE" : "INACTIVE"));
    }

    /**
     * Displays an escalation alert to the driver when all braking retry
     * attempts have been exhausted and the system cannot apply brakes.
     * Driver must take manual control immediately.
     * Matches existing FaultHandler usage signature.
     */
    public void showEscalationAlert() {
        System.out.println("\n[ESCALATION ALERT] *** CRITICAL WARNING ***");
        System.out.println("[ESCALATION ALERT] AEBS braking has FAILED after all retry attempts.");
        System.out.println("[ESCALATION ALERT] TAKE MANUAL CONTROL IMMEDIATELY.");
        printCarState();
    }

    /**
     * Displays a maintenance warning to the driver when a sensor fault
     * has been detected. System is operating on redundant sensor.
     * Matches existing FaultHandler usage signature.
     */
    public void showMaintenanceWarning() {
        System.out.println("\n[MAINTENANCE WARNING] Sensor fault detected.");
        System.out.println("[MAINTENANCE WARNING] Operating on redundant sensor.");
        System.out.println("[MAINTENANCE WARNING] Please seek maintenance.");
        printCarState();
    }

    /**
     * Displays a maintenance warning with specific sensor type information.
     * Overloaded version of showMaintenanceWarning() for when sensor
     * type is known.
     * @param type the SensorType that has failed
     */
    public void showMaintenanceWarning(SensorType type) {
        System.out.println("\n[MAINTENANCE WARNING] Sensor fault detected: " + type);
        System.out.println("[MAINTENANCE WARNING] Operating on redundant "
                + type + " sensor.");
        System.out.println("[MAINTENANCE WARNING] Please seek maintenance.");
        printCarState();
    }

    /**
     * Displays a critical sensor failure alert when both primary and
     * redundant sensors of a type have failed.
     * System is entering fail-safe mode.
     * @param type the SensorType that has completely failed
     */
    public void showCriticalSensorFailure(SensorType type) {
        System.out.println("\n[CRITICAL ALERT] *** SENSOR FAILURE ***");
        System.out.println("[CRITICAL ALERT] Both primary and redundant "
                + type + " sensors have failed.");
        System.out.println("[CRITICAL ALERT] AEBS entering fail-safe mode.");
        System.out.println("[CRITICAL ALERT] TAKE MANUAL CONTROL IMMEDIATELY.");
        printCarState();
    }

    /**
     * Returns true if the AEBS system is currently active.
     * @return true if AEBS is active
     */
    public boolean isAesbActive() {
        return aesbActive;
    }

    /**
     * Prints the current vehicle state from CarState to the console.
     * Used by all alert methods to provide context alongside alerts.
     */
    private void printCarState() {
        System.out.println("[CAR STATE] Speed:        "
                + String.format("%.2f", carState.getCarSpeed()) + " m/s");
        System.out.println("[CAR STATE] Driving Mode: "
                + carState.getDrivingMode());
        System.out.println("[CAR STATE] Weather:      "
                + carState.getWeather());
        System.out.println("[CAR STATE] Light:        "
                + carState.getLight());
        System.out.println("[CAR STATE] Time:         "
                + carState.getCurrentTimeMs() + "ms");
    }
}