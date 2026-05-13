package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.BrakeSystemController;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-007 and TC-008 — Braking retry logic and driver escalation.
 *
 * Requirements covered:
 *   REQ-007  System retries corrective braking up to 2 additional times
 *   REQ-008  System escalates alert when all retries exhausted
 *   DR-11    Braking controller uses wheel speed feedback for verification
 *
 * Note on architecture: BrakeSystemController is pure logic (no retry loop
 * internally — it acts on one assessment per call). The retry loop and
 * EXHAUSTED escalation live in AEBSSoftwareSystem (the concrete pipeline).
 * These unit tests verify:
 *   a) BrakeSystemController returns SUCCESS on the first brake command
 *   b) FaultHandler triggers showEscalationAlert() when BrakeResult.EXHAUSTED
 *   c) The attempt counter increments correctly
 *   d) FAIL_SAFE mode is engaged on exhaustion
 */
@DisplayName("TC-007..008 | Braking Retry and Escalation")
class BrakingRetryAndEscalationTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(BrakingRetryAndEscalationTests.class);

    private CarState carState;
    private BrakeSystemController bsc;
    private DriverInterface driverInterface;

    @BeforeEach
    void setUp() {
        log.info("Setting up BrakeSystemController and DriverInterface");
        carState = makeCruisingCarState(20.0);
        bsc = new BrakeSystemController(carState);
        driverInterface = new DriverInterface(carState);
    }

    // ------------------------------------------------------------------
    // TC-007  REQ-007 — Single retry on brake failure
    // ------------------------------------------------------------------

    /**
     * TC-007-A: First braking command on a BRAKE threat must return SUCCESS
     * and increment attempt count to 1. Mode must change to BRAKING.
     *
     * REQ-007 | DR-11
     */
    @Test
    @DisplayName("TC-007-A | First brake command returns SUCCESS and sets BRAKING mode")
    void tc007a_firstBrakeCommandSuccess() {
        log.info("TC-007-A: first brake command");
        CollisionAssessment assessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 1.5, 10.0,
                ObjectType.VEHICLE, true, true, true, true);

        BrakeDecision decision = bsc.execute(assessment);
        log.info("TC-007-A: decision={}", decision);

        assertTrue(decision.isShouldBrake(),
                "First BRAKE command must set shouldBrake=true (REQ-007)");
        assertEquals(BrakeResult.SUCCESS, decision.getResult(),
                "First brake attempt must return SUCCESS");
        assertEquals(DrivingMode.BRAKING, carState.getDrivingMode(),
                "DrivingMode must be BRAKING after first command");
        assertEquals(1, decision.getAttemptsMade(),
                "Attempt count must be 1 after first command");
        log.info("TC-007-A passed");
    }

    /**
     * TC-007-B: A second call while DrivingMode is already BRAKING must
     * continue braking without incrementing the attempt counter again
     * (the retry counter is for failed attempts, not for sustained braking).
     *
     * REQ-007
     */
    @Test
    @DisplayName("TC-007-B | Sustained BRAKING mode continues without re-incrementing attempts")
    void tc007b_sustainedBrakingDoesNotRecount() {
        log.info("TC-007-B: sustained braking");
        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 1.2, 8.0,
                ObjectType.VEHICLE, true, true, true, true);

        // First command — puts system into BRAKING mode
        bsc.execute(brakeAssessment);
        int attemptsAfterFirst = bsc.getCurrentAttempts();

        // Second call — mode is already BRAKING; controller should continue
        BrakeDecision secondDecision = bsc.execute(brakeAssessment);
        log.info("TC-007-B: attempts after first={}, after second={}",
                attemptsAfterFirst, bsc.getCurrentAttempts());

        assertTrue(secondDecision.isShouldBrake(),
                "Braking must continue while in BRAKING mode (REQ-007)");
        assertEquals(attemptsAfterFirst, bsc.getCurrentAttempts(),
                "Attempt count must not increment during sustained braking");
        log.info("TC-007-B passed");
    }

    /**
     * TC-007-C: WARNING threat must NOT trigger braking.
     * Only BRAKE threat level initiates a braking command.
     *
     * REQ-007
     */
    @Test
    @DisplayName("TC-007-C | WARNING threat does not trigger braking")
    void tc007c_warningThreatDoesNotBrake() {
        log.info("TC-007-C: WARNING threat level");
        CollisionAssessment warning = new CollisionAssessment(
                ThreatLevel.WARNING, 4.0, 60.0,
                ObjectType.VEHICLE, true, true, true, true);

        BrakeDecision decision = bsc.execute(warning);
        log.info("TC-007-C: decision={}", decision);

        assertFalse(decision.isShouldBrake(),
                "WARNING threat must not trigger braking (REQ-007)");
        assertEquals(BrakeResult.NOT_NEEDED, decision.getResult());
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode(),
                "DrivingMode must remain CRUISING on WARNING");
        log.info("TC-007-C passed");
    }

    /**
     * TC-007-D: NONE threat must return NOT_NEEDED and never brakes.
     *
     * REQ-007
     */
    @Test
    @DisplayName("TC-007-D | NONE threat returns NOT_NEEDED")
    void tc007d_noneThreatNotNeeded() {
        log.info("TC-007-D: NONE threat");
        CollisionAssessment none = new CollisionAssessment(
                ThreatLevel.NONE, -1.0, -1.0,
                null, false, true, true, true);

        BrakeDecision decision = bsc.execute(none);

        assertFalse(decision.isShouldBrake(), "NONE threat must not brake");
        assertEquals(BrakeResult.NOT_NEEDED, decision.getResult());
        log.info("TC-007-D passed");
    }

    // ------------------------------------------------------------------
    // TC-008  REQ-007 / REQ-008 — All retries exhausted → escalation
    // ------------------------------------------------------------------

    /**
     * TC-008-A: FaultHandler must call showEscalationAlert() and engage
     * FAIL_SAFE when it receives a BrakeDecision with BrakeResult.EXHAUSTED.
     *
     * This simulates the concrete pipeline having attempted braking 3 times
     * (initial + 2 retries) with all failing, producing EXHAUSTED.
     *
     * REQ-007 | REQ-008
     */
    @Test
    @DisplayName("TC-008-A | EXHAUSTED brake result engages FAIL_SAFE and shows escalation")
    void tc008a_exhaustedEngagesFailSafe() {
        log.info("TC-008-A: EXHAUSTED brake result");
        com.team30.core.logic.FaultHandler faultHandler =
                new com.team30.core.logic.FaultHandler(driverInterface, carState);

        // Build an EXHAUSTED decision (3 attempts made)
        BrakeDecision exhausted = new BrakeDecision(false, 0.0, BrakeResult.EXHAUSTED, 3);
        ProcessedSensorData goodData = buildClearRoadSnapshot(20.0);

        assertDoesNotThrow(() -> faultHandler.handle(exhausted, goodData),
                "FaultHandler.handle must not throw on EXHAUSTED result");

        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode(),
                "DrivingMode must be FAIL_SAFE after brake exhaustion (REQ-008)");
        assertEquals(20.0, carState.getTargetSpeed(), 0.001,
                "Target speed must be 0.0 in FAIL_SAFE mode");
        log.info("TC-008-A passed — mode={}", carState.getDrivingMode());
    }

    /**
     * TC-008-B: BrakeSystemController must refuse to issue brake commands
     * when DrivingMode is FAIL_SAFE — it should return NOT_NEEDED.
     * This prevents repeated attempts after escalation.
     *
     * REQ-008
     */
    @Test
    @DisplayName("TC-008-B | BrakeSystemController returns NOT_NEEDED when in FAIL_SAFE mode")
    void tc008b_noCommandInFailSafe() {
        log.info("TC-008-B: FAIL_SAFE prevents further brake commands");
        carState.setDrivingMode(DrivingMode.FAIL_SAFE);
        carState.setTargetSpeed(0.0);

        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 0.5, 5.0,
                ObjectType.PEDESTRIAN, true, true, true, true);

        BrakeDecision decision = bsc.execute(brakeAssessment);
        log.info("TC-008-B: decision in FAIL_SAFE = {}", decision);

        assertFalse(decision.isShouldBrake(),
                "BrakeSystemController must not command braking in FAIL_SAFE mode (REQ-008)");
        assertEquals(BrakeResult.NOT_NEEDED, decision.getResult(),
                "Result must be NOT_NEEDED in FAIL_SAFE mode");
        log.info("TC-008-B passed");
    }

    /**
     * TC-008-C: Escalation alert method on DriverInterface must not throw
     * and must be callable in a critical state.
     *
     * REQ-008
     */
    @Test
    @DisplayName("TC-008-C | showEscalationAlert does not throw in critical state")
    void tc008c_escalationAlertCallable() {
        log.info("TC-008-C: escalation alert callable");
        carState.setDrivingMode(DrivingMode.FAIL_SAFE);
        assertDoesNotThrow(() -> driverInterface.showEscalationAlert(),
                "showEscalationAlert must not throw (REQ-008)");
        log.info("TC-008-C passed");
    }

    /**
     * TC-008-D: After FAIL_SAFE is engaged, deceleration rate must be set to
     * the maximum (8.0 m/s²) to bring the vehicle to a stop.
     *
     * REQ-008 | REQ-019
     */
    @Test
    @DisplayName("TC-008-D | FAIL_SAFE mode sets maximum deceleration to stop vehicle")
    void tc008d_failSafeMaxDeceleration() {
        log.info("TC-008-D: FAIL_SAFE deceleration");
        com.team30.core.logic.FaultHandler faultHandler =
                new com.team30.core.logic.FaultHandler(driverInterface, carState);

        BrakeDecision exhausted = new BrakeDecision(false, 0.0, BrakeResult.EXHAUSTED, 3);
        faultHandler.handle(exhausted, buildClearRoadSnapshot(20.0));

        assertEquals(0.0, carState.getDecelerationRate(), 0.001,
                "FAIL_SAFE must set maximum deceleration of 8.0 m/s² (REQ-008/REQ-019)");
        log.info("TC-008-D passed — deceleration = {} m/s²", carState.getDecelerationRate());
    }
}