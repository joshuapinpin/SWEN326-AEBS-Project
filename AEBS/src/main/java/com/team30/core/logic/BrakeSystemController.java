package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.ThreatLevel;
import com.team30.simulation.state.CarState;

/**
 * The BrakeSystemController class is responsible for controlling the braking system of the vehicle
 * based on the collision assessment provided by the CollisionDetector.
 */
public class BrakeSystemController {
    private static final double BRAKE_DECELERATION = 8.0;

    private final CarState carState;
    private int currentAttempts;
    private int remainingBrakeFailures = 0;

    public BrakeSystemController(CarState carState) {
        this.carState = carState;
        this.currentAttempts = 0;
    }

    /**
     * Executes the braking decision based on the collision assessment. It considers the current driving mode,
     * the threat level, and any previous brake command attempts to determine whether to apply brakes and at what deceleration rate.
     * If the vehicle is already braking, retries are managed using remainingBrakeFailures, returning FAILED on each
     * unsuccessful attempt and EXHAUSTED once attempts reach 3. If the threat level is BRAKE, delegates to commandBrake()
     * to initiate braking. If the threat level is WARNING or NONE, no braking action is taken and NOT_NEEDED is returned.
     *
     * @param assessment the collision assessment containing the threat level and other relevant information for making braking decisions
     * @return a BrakeDecision object containing the decision to brake, the deceleration rate, the result of the braking attempt, and the number of attempts made
     */
    public BrakeDecision execute(CollisionAssessment assessment) {
        ThreatLevel threat = assessment.getThreatLevel();

        if (carState.getDrivingMode() == DrivingMode.FAIL_SAFE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
        }

        if (carState.getDrivingMode() == DrivingMode.BRAKING) {

            if (threat == ThreatLevel.NONE && (assessment.getDistance() < 0 || !assessment.isObjectInLane())) {
                carState.setDrivingMode(DrivingMode.RESUMING);
                carState.setDecelerationRate(0.0);
                currentAttempts = 0;

                return new BrakeDecision(false, 0.0, BrakeResult.CLEARED, currentAttempts);
            }

            if (remainingBrakeFailures > 0) {
                currentAttempts++;
                remainingBrakeFailures--;
                carState.setDecelerationRate(0.0);

                if (currentAttempts >= 3) {
                    return new BrakeDecision(true, 0.0, BrakeResult.EXHAUSTED, currentAttempts);
                }
                return new BrakeDecision(true, 0.0, BrakeResult.FAILED, currentAttempts);
            }

            carState.setDecelerationRate(BRAKE_DECELERATION);

            if (carState.getCarSpeed() <= 0.1) {
                carState.setDecelerationRate(0.0);
                return new BrakeDecision(false, 0.0, BrakeResult.SUCCESS, currentAttempts);
            }

            return new BrakeDecision(true, BRAKE_DECELERATION, BrakeResult.SUCCESS, currentAttempts);
        }

        if (threat == ThreatLevel.NONE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
        }

        if (threat == ThreatLevel.WARNING) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
        }

        if (threat == ThreatLevel.BRAKE) {
            return commandBrake();
        }

        return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
    }

    /**
     * Commands the braking system to apply brakes at the defined deceleration rate.
     * @return
     */
    private BrakeDecision commandBrake() {
        currentAttempts++;

        if (remainingBrakeFailures > 0) {
            remainingBrakeFailures--;
            carState.setDrivingMode(DrivingMode.BRAKING);
            carState.setDecelerationRate(0.0);
            return new BrakeDecision(true, 0.0, BrakeResult.FAILED, currentAttempts);
        }

        carState.setDrivingMode(DrivingMode.BRAKING);
        carState.setDecelerationRate(BRAKE_DECELERATION);

        return new BrakeDecision(true, BRAKE_DECELERATION, BrakeResult.SUCCESS, currentAttempts);
    }

    /** Getters and Setters */
    public int getCurrentAttempts() {
        return currentAttempts;
    }
    public long getBrakeCommandTimeMs() {
        return 0;
    }
    public void setBrakeFailures(int count) {
        remainingBrakeFailures = count;
    }

}