package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-001 to TC-005 — Driver Interface alerts and camera object classification.
 *
 * Requirements covered:
 *   REQ-001  Auditory alert on hazard detection
 *   REQ-002  Visual alert when AEB activates
 *   REQ-003  Manual AEBS enable/disable button
 *   REQ-005  Camera object classification
 *   DR-06    Per-object-type braking thresholds
 *   DR-07    Multi-level threat classification
 */
@DisplayName("TC-001..005 | Driver Alerts and Camera Classification")
class DriverAlertAndCameraTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(DriverAlertAndCameraTests.class);

    private CarState carState;
    private DriverInterface driverInterface;
    private CollisionDetector detector;

    @BeforeEach
    void setUp() {
        log.info("Setting up CarState and DriverInterface");
        carState = makeCruisingCarState(16.67); // 60 km/h
        driverInterface = new DriverInterface(carState);
        detector = new CollisionDetector();
    }

    // ------------------------------------------------------------------
    // TC-001  REQ-001 — Auditory alert fires for a hazard in warning zone
    // ------------------------------------------------------------------

    /**
     * TC-001-A: Object detected at 40 m closing at 16.67 m/s.
     * Expected: CollisionDetector returns BRAKE threat level (within stopping
     * distance). DriverInterface.emitAuditoryAlert() must not throw and the
     * auditory alert path is reachable (no exception = pass for this unit test;
     * integration-level timing is covered by TC-004).
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-001-A | Auditory alert reachable when hazard in warning zone")
    void tc001a_auditoryAlertReachableForHazard() {
        log.info("TC-001-A: testing auditory alert reachability");
        ProcessedSensorData snap = buildHazardSnapshot(40.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("Assessment: {}", assessment);

        // The object is at 40 m, closing at 16.67 m/s.
        // stoppingDist = (16.67² / 16) + 5 = ~22.4 m  →  40 m > 22.4 m → WARNING zone
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Threat level must not be NONE for a vehicle at 40 m closing at 60 km/h");

        // Verify auditory alert can be called without exception
        assertDoesNotThrow(() -> driverInterface.emitAuditoryAlert(),
                "emitAuditoryAlert must not throw");
        log.info("TC-001-A passed");
    }

    /**
     * TC-001-B: Object at 10 m closing at 16.67 m/s — within braking distance.
     * Threat must be BRAKE so the auditory alert is meaningful.
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-001-B | BRAKE threat triggers when object within stopping distance")
    void tc001b_brakeThreatWithinStoppingDistance() {
        log.info("TC-001-B: object at 10 m closing at 16.67 m/s");
        // requiredBrakeDistance(16.67) ≈ 22.4 m, object at 10 m → BRAKE
        ProcessedSensorData snap = buildHazardSnapshot(10.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("Threat: {}", assessment.getThreatLevel());

        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "Object at 10 m within braking distance must produce BRAKE threat");
        assertDoesNotThrow(() -> driverInterface.emitAuditoryAlert());
        log.info("TC-001-B passed");
    }

    // ------------------------------------------------------------------
    // TC-002  REQ-002 — Visual alert fires when AEB activates
    // ------------------------------------------------------------------

    /**
     * TC-002: When threat level is BRAKE, showBrakingActivated() must be
     * reachable without exception. In the full pipeline this is called by the
     * concrete AEBSSoftwareSystem; here we test the presentation method directly.
     *
     * REQ-002 | DR-07
     */
    @Test
    @DisplayName("TC-002 | Visual alert reachable on AEB activation (BRAKE threat)")
    void tc002_visualAlertOnBrakingActivated() {
        log.info("TC-002: verifying visual alert method is reachable");
        assertDoesNotThrow(() -> driverInterface.showBrakingActivated(),
                "showBrakingActivated must not throw");
        assertDoesNotThrow(() -> driverInterface.showVisualAlert(),
                "showVisualAlert must not throw");
        log.info("TC-002 passed");
    }

    // ------------------------------------------------------------------
    // TC-003  REQ-003 — Manual AEBS toggle
    // ------------------------------------------------------------------

    /**
     * TC-003-A: AEBS starts active by default after construction.
     *
     * REQ-003
     */
    @Test
    @DisplayName("TC-003-A | AEBS is active by default on startup")
    void tc003a_aesbActiveByDefault() {
        log.info("TC-003-A: AEBS default state");
        assertTrue(driverInterface.isAesbActive(),
                "AEBS must be active by default on startup (REQ-003)");
        log.info("TC-003-A passed");
    }

    /**
     * TC-003-B: Toggling AEBS off must set isAesbActive() to false.
     *
     * REQ-003
     */
    @Test
    @DisplayName("TC-003-B | AEBS can be deactivated via toggle")
    void tc003b_aesbCanBeDeactivated() {
        log.info("TC-003-B: deactivating AEBS via toggle");
        driverInterface.toggleAEBS(false);
        assertFalse(driverInterface.isAesbActive(),
                "AEBS must be inactive after toggleAEBS(false) (REQ-003)");
        log.info("TC-003-B passed");
    }

    /**
     * TC-003-C: Toggling AEBS off then on must restore active state.
     *
     * REQ-003
     */
    @Test
    @DisplayName("TC-003-C | AEBS re-activation after deactivation")
    void tc003c_aesbReactivation() {
        log.info("TC-003-C: deactivate then re-activate AEBS");
        driverInterface.toggleAEBS(false);
        driverInterface.toggleAEBS(true);
        assertTrue(driverInterface.isAesbActive(),
                "AEBS must be active after toggleAEBS(true) (REQ-003)");
        log.info("TC-003-C passed");
    }

    // ------------------------------------------------------------------
    // TC-005  REQ-005 — Camera object classification correctness
    // ------------------------------------------------------------------

    /**
     * TC-005-A: CameraData constructed as PEDESTRIAN in-lane must report
     * PEDESTRIAN classification. CollisionDetector must pick it up.
     *
     * REQ-005 | DR-06
     */
    @Test
    @DisplayName("TC-005-A | Camera classifies PEDESTRIAN correctly")
    void tc005a_cameraClassifiesPedestrian() {
        log.info("TC-005-A: pedestrian classification");
        long ts = System.currentTimeMillis();
        CameraData cam = new CameraData(SensorId.PRIMARY, ts,
                ObjectType.PEDESTRIAN, true, 0.92);

        assertEquals(ObjectType.PEDESTRIAN, cam.getClassification(),
                "Camera must return PEDESTRIAN classification (REQ-005)");
        assertTrue(cam.isInCurrentLane(), "Pedestrian marked in-lane");
        assertFalse(cam.isGarbage(), "Valid camera data must not be garbage");
        log.info("TC-005-A passed");
    }

    /**
     * TC-005-B: CameraData constructed as VEHICLE in-lane with high confidence
     * must be classified as VEHICLE, not UNKNOWN.
     *
     * REQ-005
     */
    @Test
    @DisplayName("TC-005-B | Camera classifies VEHICLE correctly")
    void tc005b_cameraClassifiesVehicle() {
        log.info("TC-005-B: vehicle classification");
        long ts = System.currentTimeMillis();
        CameraData cam = new CameraData(SensorId.PRIMARY, ts,
                ObjectType.VEHICLE, true, 0.97);

        assertEquals(ObjectType.VEHICLE, cam.getClassification(),
                "Camera must return VEHICLE classification (REQ-005)");
        log.info("TC-005-B passed");
    }

    /**
     * TC-005-C: Garbage CameraData must have isGarbage() = true and
     * classification = UNKNOWN, confirming failed-sensor behaviour.
     *
     * REQ-005
     */
    @Test
    @DisplayName("TC-005-C | Garbage camera data is correctly flagged")
    void tc005c_garbageCameraFlagged() {
        log.info("TC-005-C: garbage camera data");
        long ts = System.currentTimeMillis();
        CameraData garbage = new CameraData(SensorId.PRIMARY, ts);

        assertTrue(garbage.isGarbage(),
                "Garbage-constructed CameraData must have isGarbage() = true");
        assertEquals(ObjectType.UNKNOWN, garbage.getClassification(),
                "Garbage camera must classify as UNKNOWN");
        assertEquals(0.0, garbage.getConfidence(), 0.001,
                "Garbage camera confidence must be 0.0");
        log.info("TC-005-C passed");
    }

    /**
     * TC-005-D: CollisionDetector must use camera classification when assessing
     * an in-lane pedestrian — objectType in the resulting assessment must be PEDESTRIAN.
     *
     * REQ-005 | DR-06
     */
    @Test
    @DisplayName("TC-005-D | CollisionDetector uses camera classification for pedestrian")
    void tc005d_detectorUsesCamera() {
        log.info("TC-005-D: detector picks up pedestrian from camera");
        ProcessedSensorData snap = buildHazardSnapshot(
                10.0, 16.67, ObjectType.PEDESTRIAN, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("Assessment: {}", assessment);

        assertEquals(ObjectType.PEDESTRIAN, assessment.getObjectType(),
                "Assessment object type must reflect camera classification (REQ-005)");
        log.info("TC-005-D passed");
    }
}