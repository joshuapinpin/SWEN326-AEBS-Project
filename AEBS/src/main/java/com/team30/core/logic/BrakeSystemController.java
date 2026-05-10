package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.BrakeResult;
import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.ThreatLevel;
import com.team30.simulation.state.CarState;

public class BrakeSystemController {

    private static final double BRAKE_DECELERATION = 8.0;

    private final CarState carState;
    private int currentAttempts;

    private int remainingBrakeFailures = 0;


    public BrakeSystemController(CarState carState) {
        this.carState = carState;
        this.currentAttempts = 0;
    }

    public void setBrakeFailures(int count) {
        remainingBrakeFailures = count;
    }

    public BrakeDecision execute(CollisionAssessment assessment) {
        ThreatLevel threat = assessment.getThreatLevel();

        if (carState.getDrivingMode() == DrivingMode.FAIL_SAFE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
        }

        if (carState.getDrivingMode() == DrivingMode.BRAKING) {

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

    public int getCurrentAttempts() {
        return currentAttempts;
    }


    public long getBrakeCommandTimeMs() {
        return 0;
    }
}