package com.team30;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.BrakeSystemController;
import com.team30.simulation.state.CarState;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BrakeSystemControllerTest {

    @Test
    public void testFailSafeReturnsNotNeeded() {

        CarState state =
                new CarState(
                        30,
                        0,
                        new double[4],
                        DrivingMode.FAIL_SAFE,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.BRAKE,
                        1.0,
                        5.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());

        assertEquals(
                BrakeResult.NOT_NEEDED,
                result.getResult()
        );
    }

    @Test
    public void testThreatNoneDoesNotBrake() {

        CarState state =
                new CarState(
                        30,
                        0,
                        new double[4],
                        DrivingMode.CRUISING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.NONE,
                        5.0,
                        100.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());

        assertEquals(
                BrakeResult.NOT_NEEDED,
                result.getResult()
        );
    }

    @Test
    public void testThreatWarningDoesNotBrake() {

        CarState state =
                new CarState(
                        30,
                        0,
                        new double[4],
                        DrivingMode.CRUISING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.WARNING,
                        4.0,
                        50.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());

        assertEquals(
                BrakeResult.NOT_NEEDED,
                result.getResult()
        );
    }

    @Test
    public void testThreatBrakeActivatesBrake() {

        CarState state =
                new CarState(
                        30,
                        0,
                        new double[4],
                        DrivingMode.CRUISING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.BRAKE,
                        1.0,
                        5.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertTrue(result.isShouldBrake());

        assertEquals(
                BrakeResult.SUCCESS,
                result.getResult()
        );

        assertEquals(
                DrivingMode.BRAKING,
                state.getDrivingMode()
        );
    }

    @Test
    public void testBrakeAttemptsIncrement() {

        CarState state =
                new CarState(
                        30,
                        0,
                        new double[4],
                        DrivingMode.CRUISING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.BRAKE,
                        1.0,
                        5.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertEquals(
                1,
                result.getAttemptsMade()
        );
    }

    @Test
    public void testBrakingModeMaintainsBraking() {

        CarState state =
                new CarState(
                        20,
                        0,
                        new double[4],
                        DrivingMode.BRAKING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.BRAKE,
                        1.0,
                        5.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertTrue(result.isShouldBrake());

        assertEquals(
                8.0,
                result.getTargetDeceleration()
        );
    }

    @Test
    public void testVehicleStoppedEndsBraking() {

        CarState state =
                new CarState(
                        0.05,
                        0,
                        new double[4],
                        DrivingMode.BRAKING,
                        0,
                        0,
                        null,
                        null
                );

        BrakeSystemController controller =
                new BrakeSystemController(state);

        CollisionAssessment assessment =
                new CollisionAssessment(
                        ThreatLevel.BRAKE,
                        1.0,
                        1.0,
                        ObjectType.VEHICLE,
                        true,
                        true,
                        true,
                        true
                );

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());

        assertEquals(
                BrakeResult.SUCCESS,
                result.getResult()
        );
    }
}
