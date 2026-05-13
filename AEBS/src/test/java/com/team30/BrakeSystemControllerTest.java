package com.team30.core.logic;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.enums.*;
import com.team30.simulation.state.CarState;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class BrakeSystemControllerTest {

    private static final Logger log =
            LogManager.getLogger(BrakeSystemControllerTest.class);

    @Test
    public void testFailSafeReturnsNotNeeded() {

        log.info("STARTING: testFailSafeReturnsNotNeeded");

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

        log.debug("Executing controller in FAIL_SAFE mode");

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());
        assertEquals(BrakeResult.NOT_NEEDED, result.getResult());

        log.info("ENDING: testFailSafeReturnsNotNeeded");
    }

    @Test
    public void testThreatNoneDoesNotBrake() {

        log.info("STARTING: testThreatNoneDoesNotBrake");

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

        log.debug("Executing controller with ThreatLevel.NONE");

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());
        assertEquals(BrakeResult.NOT_NEEDED, result.getResult());

        log.info("ENDING: testThreatNoneDoesNotBrake");
    }

    @Test
    public void testThreatWarningDoesNotBrake() {

        log.info("STARTING: testThreatWarningDoesNotBrake");

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

        log.debug("Executing controller with ThreatLevel.WARNING");

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());
        assertEquals(BrakeResult.NOT_NEEDED, result.getResult());

        log.info("ENDING: testThreatWarningDoesNotBrake");
    }

    @Test
    public void testThreatBrakeActivatesBrake() {

        log.info("STARTING: testThreatBrakeActivatesBrake");

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

        log.debug("Executing controller with ThreatLevel.BRAKE");

        BrakeDecision result =
                controller.execute(assessment);

        assertTrue(result.isShouldBrake());
        assertEquals(BrakeResult.SUCCESS, result.getResult());
        assertEquals(DrivingMode.BRAKING, state.getDrivingMode());

        log.info("ENDING: testThreatBrakeActivatesBrake");
    }

    @Test
    public void testBrakeAttemptsIncrement() {

        log.info("STARTING: testBrakeAttemptsIncrement");

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

        log.debug("Executing controller to check brake attempt count");

        BrakeDecision result =
                controller.execute(assessment);

        assertEquals(1, result.getAttemptsMade());

        log.info("ENDING: testBrakeAttemptsIncrement");
    }

    @Test
    public void testBrakingModeMaintainsBraking() {

        log.info("STARTING: testBrakingModeMaintainsBraking");

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

        log.debug("Executing controller while already in BRAKING mode");

        BrakeDecision result =
                controller.execute(assessment);

        assertTrue(result.isShouldBrake());
        assertEquals(8.0, result.getTargetDeceleration());

        log.info("ENDING: testBrakingModeMaintainsBraking");
    }

    @Test
    public void testVehicleStoppedEndsBraking() {

        log.info("STARTING: testVehicleStoppedEndsBraking");

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

        log.debug("Executing controller with nearly stopped vehicle");

        BrakeDecision result =
                controller.execute(assessment);

        assertFalse(result.isShouldBrake());
        assertEquals(BrakeResult.SUCCESS, result.getResult());

        log.info("ENDING: testVehicleStoppedEndsBraking");
    }
}
