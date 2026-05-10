package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.ThreatLevel;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BrakeSystemController {
    //private static final Logger logger = LogManager.getLogger(BrakeSystemController.class);
    private static final int    MAX_RETRIES         = 2;
    public static final double WHEEL_CIRCUMFERENCE = 2.0;  // metres
    private static final double WARN_DECELERATION   = 3.0;  // m/s²
    private static final double BRAKE_DECELERATION  = 8.0;  // m/s²
    private static final double DECEL_TOLERANCE     = 0.05; // ±5%

    private final CarState carState;
    private int currentAttempts;
    private long brakeCommandTimeMs;
    private double speedAtLastCommand;  // wheel speed when braking was last commanded
    private double targetDecel;         // stored across ticks for verification

    public BrakeSystemController(CarState carState) {
        this.carState = carState;
        this.currentAttempts = 0;
        this.brakeCommandTimeMs = 0;
        this.speedAtLastCommand = 0.0;
        this.targetDecel = 0.0;
    }

    /**
     * Called every tick by AEBSSoftwareSystem.
     *
     * First call with a threat: commands braking, stores speed snapshot.
     * Subsequent calls: verifies deceleration against snapshot from previous tick.
     *
     * - NONE while BRAKING → CLEARED, set RESUMING
     * - NONE otherwise     → NOT_NEEDED
     * - WARNING            → soft brake at WARN_DECELERATION
     * - BRAKE              → hard brake at BRAKE_DECELERATION
     */
    public BrakeDecision execute(CollisionAssessment assessment) {
        ThreatLevel threat = assessment.getThreatLevel();

        // Threat cleared while braking — resume
        if (threat == ThreatLevel.NONE) {
            if (carState.getDrivingMode() == DrivingMode.BRAKING) {
                carState.setDrivingMode(DrivingMode.RESUMING);
                carState.setDecelerationRate(0.0);
                reset();
                return new BrakeDecision(false, 0.0, BrakeResult.CLEARED, currentAttempts);
            }
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        }

        // Don't interfere with fail-safe
        if (carState.getDrivingMode() == DrivingMode.FAIL_SAFE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        }

        targetDecel = (threat == ThreatLevel.BRAKE)
                ? BRAKE_DECELERATION
                : WARN_DECELERATION;

        // First attempt — command braking and store speed snapshot
        if (currentAttempts == 0) {
            return commandBrake();
        }

        // Subsequent ticks — verify deceleration since last command
        double currentSpeed = carState.getCarSpeed();
        long elapsedMs = carState.getCurrentTimeMs() - brakeCommandTimeMs;

        if (elapsedMs > 0) {
            double elapsedSecs = elapsedMs / 1000.0;
            double actualDecel = (speedAtLastCommand - currentSpeed) / elapsedSecs;

            if (isWithinTolerance(actualDecel, targetDecel)) {
                return new BrakeDecision(true, targetDecel, BrakeResult.SUCCESS, currentAttempts);
            }
        }

        // Verification failed — retry if attempts remaining
        if (currentAttempts <= MAX_RETRIES) {
            return commandBrake();
        }


        // All attempts exhausted
        //logger.error("Braking EXHAUSTED after {} attempts — escalating to driver", currentAttempts);
        return new BrakeDecision(true, targetDecel, BrakeResult.EXHAUSTED, currentAttempts);
    }

    /**
     * Commands braking — sets DrivingMode, decelerationRate,
     * stores current speed and timestamp for next tick verification.
     */
    private BrakeDecision commandBrake() {
        currentAttempts++;
        brakeCommandTimeMs = carState.getCurrentTimeMs();
        speedAtLastCommand = carState.getCarSpeed();

        /*logger.info("Braking command issued — attempt {} at {}ms", currentAttempts, brakeCommandTimeMs);
        logger.debug("speedAtCommand: {}", speedAtLastCommand);
        logger.debug("targetDecel:    {}", targetDecel);*/

        carState.setDrivingMode(DrivingMode.BRAKING);
        carState.setDecelerationRate(targetDecel);
        return new BrakeDecision(true, targetDecel, BrakeResult.FAILED, currentAttempts);
    }

    /**
     * Returns true if actualDecel is within ±5% of targetDecel.
     */
    private boolean isWithinTolerance(double actualDecel, double targetDecel) {
        double lower = targetDecel * (1.0 - DECEL_TOLERANCE);
        double upper = targetDecel * (1.0 + DECEL_TOLERANCE);
        return actualDecel >= lower && actualDecel <= upper;
    }

    /**
     * Converts wheel RPM array to average wheel speed in m/s.
     */
    private double averageWheelSpeed(double[] rpm) {
        if (rpm == null || rpm.length == 0) return 0.0;
        double sum = 0.0;
        for (double r : rpm) {
            sum += r * WHEEL_CIRCUMFERENCE / 60.0;
        }
        return sum / rpm.length;
    }

    private void reset() {
        currentAttempts = 0;
        brakeCommandTimeMs = 0;
        speedAtLastCommand = 0.0;
        targetDecel = 0.0;
    }

    public int getCurrentAttempts()     { return currentAttempts; }
    public long getBrakeCommandTimeMs() { return brakeCommandTimeMs; }
}