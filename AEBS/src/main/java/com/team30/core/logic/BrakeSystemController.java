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

    public BrakeSystemController(CarState carState) {
        this.carState = carState;
        this.currentAttempts = 0;
    }

    public BrakeDecision execute(CollisionAssessment assessment) {
        ThreatLevel threat = assessment.getThreatLevel();

        if (carState.getDrivingMode() == DrivingMode.FAIL_SAFE) {
            return new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, currentAttempts);
        }

        if (carState.getDrivingMode() == DrivingMode.BRAKING) {
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