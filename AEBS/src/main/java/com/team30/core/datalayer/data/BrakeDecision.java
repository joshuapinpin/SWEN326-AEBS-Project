package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.BrakeResult;

/**
 * Represents the result of a braking attempt produced by BrakeSystemController.
 * Holds whether braking should occur, the target deceleration rate, the result
 * of the braking attempt, and how many attempts were made.
 * Passed to FaultHandler to determine whether to escalate alerts to the
 * driver or trigger fail-safe behaviour after braking retries are exhausted.
 */
public class BrakeDecision {
    private final boolean shouldBrake;
    private final double targetDeceleration; //The target deceleration rate in m/s² for this braking attempt.
    private final BrakeResult result; //The result of the braking attempt.
    /**
     * The number of braking attempts made including the initial attempt and any retries.
     * Maximum of MAX_RETRIES + 1 (3 total attempts).
     */
    private final int attemptsMade;

    /**
     * Constructs a BrakeDecision with all fields set.
     * @param shouldBrake        true if braking should engage
     * @param targetDeceleration target deceleration rate in m/s²
     * @param result             the result of the braking attempt
     * @param attemptsMade       number of braking attempts made
     */
    public BrakeDecision(boolean shouldBrake,double targetDeceleration, BrakeResult result, int attemptsMade) {
        this.shouldBrake = shouldBrake;
        this.targetDeceleration = targetDeceleration;
        this.result = result;
        this.attemptsMade = attemptsMade;
    }

    /**
     * Returns true if the braking system should engage.
     * @return true if braking should engage
     */
    public boolean isShouldBrake() { return shouldBrake; }

    /**
     * Returns the target deceleration rate in m/s².
     * @return target deceleration in m/s²
     */
    public double getTargetDeceleration() { return targetDeceleration; }

    /**
     * Returns the result of the braking attempt.
     * @return the BrakeResult of this decision
     */
    public BrakeResult getResult() { return result; }

    /**
     * Returns the number of braking attempts made.
     * @return number of attempts made
     */
    public int getAttemptsMade() { return attemptsMade; }

    /**
     * Returns a string representation of this decision for debugging.
     * @return string representation of all decision fields
     */
    @Override
    public String toString() {
        return "BrakeDecision{" +
                "shouldBrake=" + shouldBrake +
                ", targetDeceleration=" + targetDeceleration +
                ", result=" + result +
                ", attemptsMade=" + attemptsMade +
                '}';
    }
}