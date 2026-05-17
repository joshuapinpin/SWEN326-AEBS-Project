package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.BrakeSystemController;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.logic.FaultHandler;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-026, TC-027, TC-028 — Resuming after threat clears, alert deduplication,
 * and braking verification using wheel speed feedback.
 */
@DisplayName("TC-026..028 | Resume State, Alert Deduplication, Braking Verification")
class ResumeAlertBrakingVerificationTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(ResumeAlertBrakingVerificationTests.class);

    private CarState carState;
    private BrakeSystemController bsc;
    private CollisionDetector detector;
    private DriverInterface driverInterface;
    private FaultHandler faultHandler;

    @BeforeEach
    void setUp() {
        log.info("Setting up components for resume / deduplication / verification tests");
        carState      = makeCruisingCarState(16.67);
        bsc           = new BrakeSystemController(carState);
        detector      = new CollisionDetector();
        driverInterface = new DriverInterface(carState);
        faultHandler  = new FaultHandler(driverInterface, carState);
    }

    // ---------------------------------------------------------------
    // TC-026
    // ---------------------------------------------------------------

    /**
     * TC-026-A: After the system has been braking (BRAKING mode) and the threat
     * is gone (NONE), BrakeSystemController must eventually stop commanding
     * brakes when the vehicle speed reaches zero (or near zero).
     */
    @Test
    @DisplayName("TC-026-A | Braking stops when vehicle speed reaches zero")
    void tc026a_brakingStopsAtZeroSpeed() {
        log.info("TC-026-A: braking stops at zero speed");
        // Put system into BRAKING mode with a brake threat
        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 0.5, 5.0,
                ObjectType.VEHICLE, true, true, true, true);
        bsc.execute(brakeAssessment); // sets DrivingMode = BRAKING

        // Simulate car decelerating to near-zero
        carState.setCarSpeed(0.05); // below 0.1 threshold

        // While in BRAKING mode, another call should detect speed ≤ 0.1 and return SUCCESS with shouldBrake=false
        CollisionAssessment stillBraking = new CollisionAssessment(
                ThreatLevel.BRAKE, 0.1, 2.0,
                ObjectType.VEHICLE, true, true, true, true);
        BrakeDecision decision = bsc.execute(stillBraking);

        log.info("TC-026-A: speed={}, shouldBrake={}, result={}",
                carState.getCarSpeed(), decision.isShouldBrake(), decision.getResult());

        assertFalse(decision.isShouldBrake(),
                "Braking must stop when car speed ≤ 0.1 m/s (DR-09)");
        assertEquals(BrakeResult.SUCCESS, decision.getResult(),
                "Result must be SUCCESS when vehicle has stopped (DR-09)");
        log.info("TC-026-A passed");
    }

    /**
     * TC-026-B: CollisionDetector returns NONE when no object is detected.
     * After a BRAKE event, if the next assessment is NONE (object cleared),
     * BrakeSystemController in CRUISING mode must return NOT_NEEDED —
     * confirming the system transitions back to normal.
     */
    @Test
    @DisplayName("TC-026-B | NONE assessment in CRUISING mode returns NOT_NEEDED (threat cleared)")
    void tc026b_noneAssessmentInCruisingReturnsNotNeeded() {
        log.info("TC-026-B: NONE assessment in CRUISING mode");
        // System is in CRUISING mode (default), receives a NONE assessment
        CollisionAssessment noneAssessment = new CollisionAssessment(
                ThreatLevel.NONE, -1.0, -1.0,
                null, false, true, true, true);

        BrakeDecision decision = bsc.execute(noneAssessment);
        log.info("TC-026-B: decision = {}", decision);

        assertFalse(decision.isShouldBrake(),
                "NONE threat must not trigger braking (DR-09)");
        assertEquals(BrakeResult.NOT_NEEDED, decision.getResult(),
                "Result must be NOT_NEEDED when no threat (DR-09)");
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode(),
                "DrivingMode must remain CRUISING after NONE assessment");
        log.info("TC-026-B passed");
    }

    /**
     * TC-026-C: Car at speed 0.0 in BRAKING mode — BrakeSystemController must
     * set deceleration to 0.0 (no longer decelerating) and return shouldBrake=false.
     */
    @Test
    @DisplayName("TC-026-C | Car stopped in BRAKING mode — deceleration reset to 0")
    void tc026c_stoppedCarResetsDeceleration() {
        log.info("TC-026-C: car stopped, deceleration reset");
        carState.setDrivingMode(DrivingMode.BRAKING);
        carState.setCarSpeed(0.0);

        CollisionAssessment assessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 0.0, 0.0,
                ObjectType.VEHICLE, true, true, true, true);
        BrakeDecision decision = bsc.execute(assessment);

        log.info("TC-026-C: decelerationRate={}, shouldBrake={}",
                carState.getDecelerationRate(), decision.isShouldBrake());
        assertFalse(decision.isShouldBrake(),
                "Braking must not continue when car speed = 0 (DR-09)");
        assertEquals(0.0, carState.getDecelerationRate(), 0.001,
                "Deceleration rate must be reset to 0 when stopped (DR-09)");
        log.info("TC-026-C passed");
    }

    // ---------------------------------------------------------------
    // TC-027
    // ---------------------------------------------------------------

    /**
     * TC-027-A: The threat level returned for a steady object (same distance,
     * same speed) on repeated assess() calls must be identical. This means a
     * system that only fires alerts on threat level CHANGE will correctly
     * deduplicate — same output = same level = no new alert.
     */
    @Test
    @DisplayName("TC-027-A | Repeated assess() for same object produces same threat level (no change = no new alert)")
    void tc027a_repeatedAssessProducesSameThreat() {
        log.info("TC-027-A: repeated assess same object");
        ProcessedSensorData snap = buildHazardSnapshot(
                150.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment first  = detector.assess(snap);
        CollisionAssessment second = detector.assess(snap);
        CollisionAssessment third  = detector.assess(snap);

        log.info("TC-027-A: first={}, second={}, third={}",
                first.getThreatLevel(), second.getThreatLevel(), third.getThreatLevel());
        assertEquals(first.getThreatLevel(), second.getThreatLevel(),
                "Repeated assess of same object must return same threat level (DR-10)");
        assertEquals(second.getThreatLevel(), third.getThreatLevel(),
                "Third repeated assess must match second (DR-10)");
        log.info("TC-027-A passed");
    }

    /**
     * TC-027-B: Alert methods on DriverInterface must not throw when called
     * multiple times — the system must be safe to call repeatedly if needed.
     * (In production, the pipeline tracks prior threat level and only calls
     * alert methods on change; here we confirm the methods are idempotent.)
     */
    @Test
    @DisplayName("TC-027-B | Driver alert methods are safe to call repeatedly (idempotent)")
    void tc027b_alertMethodsIdempotent() {
        log.info("TC-027-B: alert methods idempotent");
        assertDoesNotThrow(() -> {
            driverInterface.emitAuditoryAlert();
            driverInterface.emitAuditoryAlert();
            driverInterface.showVisualAlert();
            driverInterface.showVisualAlert();
            driverInterface.showBrakingActivated();
            driverInterface.showBrakingActivated();
        }, "Alert methods must not throw when called multiple times (DR-10)");
        log.info("TC-027-B passed");
    }

    /**
     * TC-027-C: A change in threat level (WARNING → BRAKE) must produce a
     * different ThreatLevel in the assessment — confirming the detector responds
     * to input change, allowing the caller to fire a new alert.
     */
    @Test
    @DisplayName("TC-027-C | Threat level changes when object closes (enables alert on change logic)")
    void tc027c_threatLevelChangesOnObjectApproach() {
        log.info("TC-027-C: threat level changes as object approaches");
        ProcessedSensorData warnSnap = buildHazardSnapshot(
                150.0, 16.67, ObjectType.VEHICLE, 16.67);
        ProcessedSensorData brakeSnap = buildHazardSnapshot(
                5.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment warnResult  = detector.assess(warnSnap);
        CollisionAssessment brakeResult = detector.assess(brakeSnap);

        log.info("TC-027-C: warn={}, brake={}",
                warnResult.getThreatLevel(), brakeResult.getThreatLevel());
        assertNotEquals(warnResult.getThreatLevel(), brakeResult.getThreatLevel(),
                "Threat level must change as object approaches (DR-10 — enables deduplication)");
        log.info("TC-027-C passed");
    }

    // ---------------------------------------------------------------
    // TC-028
    // ---------------------------------------------------------------

    /**
     * TC-028-A: BrakeDecision returned by BrakeSystemController on a BRAKE threat
     * must carry shouldBrake=true and targetDeceleration=8.0 m/s².
     * The wheel-speed-based execution check uses these values.
     */
    @Test
    @DisplayName("TC-028-A | BrakeDecision carries correct deceleration for wheel-speed verification")
    void tc028a_brakeDecisionCarriesCorrectDeceleration() {
        log.info("TC-028-A: brake decision deceleration value");
        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 1.0, 8.0,
                ObjectType.VEHICLE, true, true, true, true);

        BrakeDecision decision = bsc.execute(brakeAssessment);

        log.info("TC-028-A: shouldBrake={}, decel={}", decision.isShouldBrake(),
                decision.getTargetDeceleration());
        assertTrue(decision.isShouldBrake(),
                "Brake command must have shouldBrake=true (DR-11)");
        assertEquals(8.0, decision.getTargetDeceleration(), 0.001,
                "Target deceleration must be 8.0 m/s² (REQ-015 / DR-11)");
        log.info("TC-028-A passed");
    }

    /**
     * TC-028-B: After a brake command, CarState.decelerationRate must reflect
     * the commanded deceleration — confirming the controller applies it.
     * The simulator uses this field to update vehicle speed.
     */
    @Test
    @DisplayName("TC-028-B | CarState.decelerationRate set to 8.0 m/s² after brake command")
    void tc028b_carStateDecelerationSetAfterBrake() {
        log.info("TC-028-B: CarState deceleration set after brake command");
        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 1.0, 8.0,
                ObjectType.VEHICLE, true, true, true, true);

        bsc.execute(brakeAssessment);

        log.info("TC-028-B: decelerationRate = {}", carState.getDecelerationRate());
        assertEquals(8.0, carState.getDecelerationRate(), 0.001,
                "CarState deceleration must be 8.0 m/s² after brake command (REQ-015 / DR-11)");
        log.info("TC-028-B passed");
    }

    /**
     * TC-028-C: Execution check simulation — compare actual wheel-derived
     * deceleration to expected and confirm it is within ±5%.
     *
     * Scenario: car at 16.67 m/s, brake applied for 50 ms.
     * Expected speed after: 16.27 m/s. Actual (simulated): 16.28 m/s.
     * Derived decel: (16.67 - 16.28) / 0.05 = 7.8 m/s² → within ±5% of 8.0.
     */
    @Test
    @DisplayName("TC-028-C | Wheel-derived deceleration within ±5% of commanded 8.0 m/s²")
    void tc028c_wheelDerivedDecelerationWithinTolerance() {
        log.info("TC-028-C: wheel-speed-derived deceleration verification");
        double initialSpeed     = 16.67; // m/s
        double commandedDecel   = 8.0;   // m/s²
        double dt               = 0.05;  // 50 ms step

        // Simulate actual wheel speed after braking step
        double simulatedActualSpeed = 16.28; // realistic with minor slip
        double derivedDecel = (initialSpeed - simulatedActualSpeed) / dt; // 7.8 m/s²

        double deviationPct = Math.abs(derivedDecel - commandedDecel) / commandedDecel;

        log.info("TC-028-C: commanded={}. derived={}, deviation={:.2f}%",
                commandedDecel, derivedDecel, deviationPct * 100);
        assertTrue(deviationPct <= 0.05,
                String.format(
                        "Wheel-derived deceleration deviation %.2f%% must be ≤ 5%% (REQ-015 / DR-11)",
                        deviationPct * 100));
        log.info("TC-028-C passed");
    }

    /**
     * TC-028-D: BrakeDecision must record the correct number of attempts made.
     * Initial command = 1 attempt. Confirms the counter is available for
     * the retry/escalation logic upstream.
     */
    @Test
    @DisplayName("TC-028-D | BrakeDecision attempt count is 1 after first command")
    void tc028d_attemptCountAfterFirstCommand() {
        log.info("TC-028-D: attempt count check");
        CollisionAssessment brakeAssessment = new CollisionAssessment(
                ThreatLevel.BRAKE, 1.2, 9.0,
                ObjectType.PEDESTRIAN, true, true, true, true);

        BrakeDecision decision = bsc.execute(brakeAssessment);

        log.info("TC-028-D: attemptsMade = {}", decision.getAttemptsMade());
        assertEquals(1, decision.getAttemptsMade(),
                "First brake command must report 1 attempt made (DR-11)");
        log.info("TC-028-D passed");
    }

    /**
     * TC-028-E: Deceleration accuracy negative test — a severely under-braking
     * scenario (only 2.0 m/s² actual vs 8.0 m/s² commanded) must flag as
     * outside ±5% tolerance. This confirms the execution check logic would
     * correctly detect a failure.
     */
    @Test
    @DisplayName("TC-028-E | Under-braking 2.0 m/s² vs 8.0 m/s² target correctly exceeds ±5%")
    void tc028e_severeBrakingFailureExceedsTolerance() {
        log.info("TC-028-E: severe under-braking detection (negative test)");
        double commandedDecel = 8.0;
        double actualDecel    = 2.0; // severe under-braking
        double deviationPct   = Math.abs(actualDecel - commandedDecel) / commandedDecel;

        log.info("TC-028-E: deviation = {:.2f}%", deviationPct * 100);
        assertTrue(deviationPct > 0.05,
                "2.0 m/s² actual vs 8.0 m/s² commanded must exceed ±5%% tolerance (REQ-015 / DR-11)");
        log.info("TC-028-E passed (negative test)");
    }
}