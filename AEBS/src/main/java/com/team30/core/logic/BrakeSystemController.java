package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.ThreatLevel;
import com.team30.simulation.state.CarState;

public class BrakeSystemController {

    private static final int    MAX_RETRIES            = 2;
    public static final double WHEEL_CIRCUMFERENCE    = 2.0;   // metres
    private static final double WARN_DECELERATION      = 3.0;   // m/s²
    private static final double BRAKE_DECELERATION     = 8.0;   // m/s²
    private static final double DECEL_TOLERANCE        = 0.05;  // ±5%
    private static final long   BRAKE_WAIT_MS          = 50;    // 5 ticks

    private final CarState carState;
    private int currentAttempts;
    private long brakeCommandTimeMs;

    public BrakeSystemController(CarState carState) {
        this.carState = carState;
        this.currentAttempts = 0;
        this.brakeCommandTimeMs = 0;
    }

    /**
     * Executes braking based on the CollisionAssessment threat level.
     *
     * - NONE while BRAKING → clear brakes, set RESUMING, return CLEARED
     * - NONE otherwise     → no action, return NOT_NEEDED
     * - WARNING            → soft brake at WARN_DECELERATION
     * - BRAKE              → hard brake at BRAKE_DECELERATION
     *
     * Retries up to MAX_RETRIES + 1 times, checking deceleration after each attempt.
     * Returns EXHAUSTED if all attempts fail.
     *
     * @param assessment the CollisionAssessment from CollisionDetector
     * @return BrakeDecision containing result, target deceleration, and attempts made
     */
    public BrakeDecision execute(CollisionAssessment assessment) {
        ThreatLevel threat = assessment.getThreatLevel();

        // If threat cleared while braking, resume normal driving
        if (threat == ThreatLevel.NONE) {
            if (carState.getDrivingMode() == DrivingMode.BRAKING) {
                carState.setDrivingMode(DrivingMode.RESUMING);
                carState.setTargetSpeed(carState.getTargetSpeed());
                carState.setDecelerationRate(0.0);
                currentAttempts = 0;
                return new BrakeDecision(false, 0.0, BrakeResult.CLEARED, 0);
            }
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        }

        // Skip if already in fail-safe — don't interfere
        if (carState.getDrivingMode() == DrivingMode.FAIL_SAFE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        }

        double targetDecel = (threat == ThreatLevel.BRAKE)
                ? BRAKE_DECELERATION
                : WARN_DECELERATION;

        currentAttempts = 0;

        // Retry loop — up to MAX_RETRIES + 1 total attempts
        while (currentAttempts <= MAX_RETRIES) {
            currentAttempts++;
            brakeCommandTimeMs = carState.getCurrentTimeMs();

            // Command braking
            carState.setDrivingMode(DrivingMode.BRAKING);
            carState.setDecelerationRate(targetDecel);

            // Read wheel speed before wait
            double speedBefore = averageWheelSpeed(carState.getWheelRPM());

            // Wait for brakes to physically respond
            try {
                Thread.sleep(BRAKE_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Read wheel speed after wait
            double speedAfter = averageWheelSpeed(carState.getWheelRPM());

            // Calculate actual deceleration over 50ms
            double actualDecel = (speedBefore - speedAfter) / (BRAKE_WAIT_MS / 1000.0);

            // Check within ±5% of target
            if (isWithinTolerance(actualDecel, targetDecel)) {
                return new BrakeDecision(true, targetDecel, BrakeResult.SUCCESS, currentAttempts);
            }

            // Failed this attempt — retry if attempts remaining
        }

        // All attempts exhausted
        return new BrakeDecision(true, targetDecel, BrakeResult.EXHAUSTED, currentAttempts);
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
     * speed = RPM * circumference / 60
     */
    private double averageWheelSpeed(double[] rpm) {
        if (rpm == null || rpm.length == 0) return 0.0;
        double sum = 0.0;
        for (double r : rpm) {
            sum += r * WHEEL_CIRCUMFERENCE / 60.0;
        }
        return sum / rpm.length;
    }

    public int getCurrentAttempts()    { return currentAttempts; }
    public long getBrakeCommandTimeMs() { return brakeCommandTimeMs; }
}
