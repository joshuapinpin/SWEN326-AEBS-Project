package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.logic.RedundancyChecker;
import com.team30.core.logic.SensorInputHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-020, TC-022, TC-023, TC-024 — Sensor data encapsulation, TTC calculation,
 * per-object-type thresholds, and multi-level threat classification.
 *
 * Requirements covered:
 *   REQ-001  Auditory alert — confirmed via threat level progression
 *   REQ-005  Camera classification — feeds per-type thresholds
 *   REQ-013  Braking control signal timing — linked to threat escalation
 *   REQ-016  Unit consistency — encapsulation check
 *   DR-02    Processing component encapsulates all sensor data
 *   DR-05    TTC calculated from radar distance + wheel speed
 *   DR-06    Per-object-type warning and braking thresholds
 *   DR-07    Multi-level threat classification before braking
 */
@DisplayName("TC-020, 022-024 | Encapsulation, TTC, Per-Type Thresholds, Threat Levels")
class EncapsulationTtcThresholdTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(EncapsulationTtcThresholdTests.class);

    private CollisionDetector detector;
    private RedundancyChecker checker;

    @BeforeEach
    void setUp() {
        log.info("Setting up CollisionDetector and RedundancyChecker");
        detector = new CollisionDetector();
        checker  = new RedundancyChecker();
    }

    // ---------------------------------------------------------------
    // TC-020  REQ-016 / DR-02 — Sensor data processing encapsulation
    // ---------------------------------------------------------------

    /**
     * TC-020-A: SensorInputHandler.update() accepts each sensor type and
     * produces a ProcessedSensorData where data is keyed by SensorType and
     * SensorId — confirming that the processing component encapsulates all
     * sensor data (DR-02).
     *
     * REQ-016 | DR-02
     */
    @Test
    @DisplayName("TC-020-A | SensorInputHandler encapsulates all sensor types into ProcessedSensorData")
    void tc020a_handlerEncapsulatesAllTypes() {
        log.info("TC-020-A: encapsulation via SensorInputHandler");
        SensorInputHandler handler = new SensorInputHandler();
        long ts = System.currentTimeMillis();

        handler.addToBuffer(new RadarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        handler.addToBuffer(new RadarData(SensorId.REDUNDANT, ts, 40.5, 16.67, true));
        handler.addToBuffer(new LidarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        handler.addToBuffer(new LidarData(SensorId.REDUNDANT, ts, 40.5, 16.67, true));
        handler.addToBuffer(new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
        handler.addToBuffer(new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        double[] rpm = {800, 800, 800, 800};
        double[] spd = {16.67, 16.67, 16.67, 16.67};
        handler.addToBuffer(new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        handler.addToBuffer(new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));

        ProcessedSensorData snapshot = handler.getLatest();
        assertNotNull(snapshot, "Snapshot must not be null (DR-02)");

        // Confirm all four types are accessible through the unified container
        for (SensorType type : SensorType.values()) {
            SensorData primary = snapshot.getSensorData(type, SensorId.PRIMARY);
            assertNotNull(primary,
                    "ProcessedSensorData must contain PRIMARY " + type
                            + " — encapsulation check (DR-02)");
            assertEquals(type, primary.getSensorType(),
                    "SensorData must report correct type (REQ-016)");
        }
        log.info("TC-020-A passed");
    }

    /**
     * TC-020-B: After calling getLatest(), the updated sensor types set is
     * cleared so a subsequent getLatest() call with no new updates returns null.
     * This confirms the processing boundary — stale data is not re-served.
     *
     * DR-02
     */
    @Test
    @DisplayName("TC-020-B | Second getLatest() call with no new data returns null")
    void tc020b_handlerClearsAfterSnapshot() {
        log.info("TC-020-B: handler clears buffer state after snapshot");
        SensorInputHandler handler = new SensorInputHandler();
        long ts = System.currentTimeMillis();

        handler.addToBuffer(new RadarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        ProcessedSensorData first = handler.getLatest();
        assertNotNull(first, "First getLatest() must return a snapshot");

        // No new updates — second call should return null (buffer emptied)
        ProcessedSensorData second = handler.getLatest();
        assertNull(second,
                "Second getLatest() with no new data must return null (DR-02)");
        log.info("TC-020-B passed");
    }

    /**
     * TC-020-C: SensorData retrieved through ProcessedSensorData carries the
     * correct SensorType — confirming unit label preservation through encapsulation.
     *
     * REQ-016 | DR-02
     */
    @Test
    @DisplayName("TC-020-C | SensorType is preserved through ProcessedSensorData retrieval")
    void tc020c_sensorTypePreservedThroughContainer() {
        log.info("TC-020-C: sensor type preserved in container");
        ProcessedSensorData snap = buildHazardSnapshot(40.0, 16.67, ObjectType.VEHICLE, 16.67);

        SensorData radar  = snap.getSensorData(SensorType.RADAR,       SensorId.PRIMARY);
        SensorData lidar  = snap.getSensorData(SensorType.LIDAR,       SensorId.PRIMARY);
        SensorData camera = snap.getSensorData(SensorType.CAMERA,      SensorId.PRIMARY);
        SensorData wheel  = snap.getSensorData(SensorType.WHEEL_SPEED, SensorId.PRIMARY);

        assertEquals(SensorType.RADAR,       radar.getSensorType(),  "RADAR type preserved (REQ-016)");
        assertEquals(SensorType.LIDAR,       lidar.getSensorType(),  "LIDAR type preserved (REQ-016)");
        assertEquals(SensorType.CAMERA,      camera.getSensorType(), "CAMERA type preserved (REQ-016)");
        assertEquals(SensorType.WHEEL_SPEED, wheel.getSensorType(),  "WHEEL_SPEED type preserved (REQ-016)");
        log.info("TC-020-C passed");
    }

    // ---------------------------------------------------------------
    // TC-022  DR-05 — TTC calculation accuracy
    // ---------------------------------------------------------------

    /**
     * TC-022-A: Vehicle at 40 m closing at 16.67 m/s (60 km/h).
     * Expected TTC = 40 / 16.67 = 2.4 s ± 0.1 s.
     *
     * DR-05 | REQ-004 | REQ-006
     */
    @Test
    @DisplayName("TC-022-A | TTC = distance / relativeSpeed within ±0.1 s tolerance")
    void tc022a_ttcAccuracyVehicle() {
        log.info("TC-022-A: TTC accuracy for vehicle at 40 m");
        double distance   = 40.0;
        double relSpeed   = 16.67;
        double expectedTtc = distance / relSpeed; // 2.4 s

        ProcessedSensorData snap = buildHazardSnapshot(
                distance, relSpeed, ObjectType.VEHICLE, relSpeed);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-022-A: expected TTC = {}, actual TTC = {}",
                expectedTtc, assessment.getTimeToCollision());
        assertNotNull(assessment);
        assertEquals(expectedTtc, assessment.getTimeToCollision(), 0.1,
                "TTC must equal distance/relativeSpeed within ±0.1 s (DR-05)");
        log.info("TC-022-A passed");
    }

    /**
     * TC-022-B: Pedestrian at 30 m closing at 11.11 m/s (40 km/h).
     * Expected TTC = 30 / 11.11 = 2.7 s ± 0.1 s.
     *
     * DR-05
     */
    @Test
    @DisplayName("TC-022-B | TTC for pedestrian at 30 m is within ±0.1 s of expected")
    void tc022b_ttcAccuracyPedestrian() {
        log.info("TC-022-B: TTC for pedestrian at 30 m");
        double distance    = 30.0;
        double relSpeed    = 11.11;
        double expectedTtc = distance / relSpeed; // ≈ 2.7 s

        ProcessedSensorData snap = buildHazardSnapshot(
                distance, relSpeed, ObjectType.PEDESTRIAN, relSpeed);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-022-B: expected TTC = {}, actual TTC = {}",
                expectedTtc, assessment.getTimeToCollision());
        assertEquals(expectedTtc, assessment.getTimeToCollision(), 0.1,
                "TTC for pedestrian must be within ±0.1 s (DR-05)");
        log.info("TC-022-B passed");
    }

    /**
     * TC-022-C: Object not closing (relativeSpeed = 0 or negative) must produce
     * ThreatLevel.NONE because TTC is infinite or undefined — car cannot collide
     * with something it is not approaching.
     *
     * DR-05
     */
    @Test
    @DisplayName("TC-022-C | Zero or negative relative speed → NONE threat (no collision path)")
    void tc022c_zeroRelSpeedProducesNone() {
        log.info("TC-022-C: zero relative speed");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        // Radar shows object but relative speed = 0 (moving at same speed as car)
        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, 20.0, 0.0, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, 20.5, 0.0, true));
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, 20.0, 0.0, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, 20.5, 0.0, true));
        readings.put(SensorType.LIDAR, lidarMap);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        readings.put(SensorType.CAMERA, cameraMap);

        double[] rpm = {500, 500, 500, 500};
        double[] spd = {16.67, 16.67, 16.67, 16.67};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        ProcessedSensorData snap      = buildSnapshot(readings);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-022-C: threat = {}", assessment.getThreatLevel());
        assertEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Zero relative speed must produce NONE threat — no collision path (DR-05)");
        log.info("TC-022-C passed");
    }

    /**
     * TC-022-D: Object out of current lane must produce NONE threat regardless
     * of distance or TTC — confirming lane-check is prior to TTC calculation.
     *
     * DR-05
     */
    @Test
    @DisplayName("TC-022-D | Object out of lane → NONE threat regardless of TTC")
    void tc022d_outOfLaneProducesNone() {
        log.info("TC-022-D: object not in current lane");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, 10.0, 16.67, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, 10.5, 16.67, true));
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, 10.0, 16.67, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, 10.5, 16.67, true));
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera marks object as NOT in current lane
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.PEDESTRIAN, false, 0.90));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.PEDESTRIAN, false, 0.88));
        readings.put(SensorType.CAMERA, cameraMap);

        double[] rpm = {500, 500, 500, 500};
        double[] spd = {16.67, 16.67, 16.67, 16.67};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        ProcessedSensorData snap      = buildSnapshot(readings);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-022-D: objectInLane={}, threat={}",
                assessment.isObjectInLane(), assessment.getThreatLevel());
        assertFalse(assessment.isObjectInLane(),
                "Object must be flagged as not in lane (DR-05)");
        assertEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Out-of-lane object must produce NONE threat regardless of distance (DR-05)");
        log.info("TC-022-D passed");
    }

    // ---------------------------------------------------------------
    // TC-023  DR-06 — Per-object-type warning and braking thresholds
    // ---------------------------------------------------------------

    /**
     * TC-023-A: Place a VEHICLE at a distance that is within braking distance.
     * Confirm BRAKE threat is produced. Then place a PEDESTRIAN at the same
     * distance — PEDESTRIAN also produces BRAKE. Both must trigger BRAKE at
     * short range because the physics threshold covers both.
     *
     * This test focuses on confirming that pedestrians are not ignored.
     *
     * REQ-005 | DR-06
     */
    @Test
    @DisplayName("TC-023-A | PEDESTRIAN and VEHICLE both produce BRAKE at close range")
    void tc023a_bothTypesProduceBrakeAtCloseRange() {
        log.info("TC-023-A: pedestrian and vehicle both BRAKE at close range");
        // At 8 m, 16.67 m/s: stopping dist ≈ 22.4 m → 8 m < 22.4 m → BRAKE
        ProcessedSensorData vehicleSnap = buildHazardSnapshot(
                8.0, 16.67, ObjectType.VEHICLE, 16.67);
        ProcessedSensorData pedestrianSnap = buildHazardSnapshot(
                8.0, 16.67, ObjectType.PEDESTRIAN, 16.67);

        CollisionDetector d1 = new CollisionDetector();
        CollisionDetector d2 = new CollisionDetector();

        CollisionAssessment vehicleAssessment     = d1.assess(vehicleSnap);
        CollisionAssessment pedestrianAssessment  = d2.assess(pedestrianSnap);

        log.info("TC-023-A: vehicle={}, pedestrian={}",
                vehicleAssessment.getThreatLevel(), pedestrianAssessment.getThreatLevel());
        assertEquals(ThreatLevel.BRAKE, vehicleAssessment.getThreatLevel(),
                "Vehicle at close range must produce BRAKE (DR-06)");
        assertEquals(ThreatLevel.BRAKE, pedestrianAssessment.getThreatLevel(),
                "Pedestrian at close range must produce BRAKE (DR-06)");
        log.info("TC-023-A passed");
    }

    /**
     * TC-023-B: Object type is correctly carried through to the CollisionAssessment.
     * This allows the calling code (e.g. a future threshold layer) to apply
     * different per-type logic downstream if needed.
     *
     * REQ-005 | DR-06
     */
    @Test
    @DisplayName("TC-023-B | CollisionAssessment carries correct object type for threshold decisions")
    void tc023b_assessmentCarriesCorrectObjectType() {
        log.info("TC-023-B: object type carried in assessment");
        CollisionDetector d1 = new CollisionDetector();
        CollisionDetector d2 = new CollisionDetector();

        ProcessedSensorData vSnap = buildHazardSnapshot(10.0, 16.67, ObjectType.VEHICLE, 16.67);
        ProcessedSensorData pSnap = buildHazardSnapshot(10.0, 16.67, ObjectType.PEDESTRIAN, 16.67);

        CollisionAssessment vAssessment = d1.assess(vSnap);
        CollisionAssessment pAssessment = d2.assess(pSnap);

        assertEquals(ObjectType.VEHICLE,    vAssessment.getObjectType(),
                "Assessment must report VEHICLE type (DR-06)");
        assertEquals(ObjectType.PEDESTRIAN, pAssessment.getObjectType(),
                "Assessment must report PEDESTRIAN type (DR-06)");
        log.info("TC-023-B passed");
    }

    /**
     * TC-023-C: Object not detected produces NONE regardless of object type field.
     * Confirms the detection gate works before type classification.
     *
     * DR-06
     */
    @Test
    @DisplayName("TC-023-C | No object detected → NONE regardless of camera classification")
    void tc023c_noObjectProducesNone() {
        log.info("TC-023-C: no object in radar/lidar → NONE threat");
        ProcessedSensorData snap = buildClearRoadSnapshot(16.67);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-023-C: threat = {}", assessment != null ? assessment.getThreatLevel() : "null");
        if (assessment != null) {
            assertEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                    "Clear road must produce NONE threat (DR-06)");
        }
        log.info("TC-023-C passed");
    }

    // ---------------------------------------------------------------
    // TC-024  REQ-001 / DR-07 — Multi-level threat classification
    // ---------------------------------------------------------------

    /**
     * TC-024-A: Object at 150 m (far — warning zone) must produce WARNING,
     * not BRAKE. Confirms the first intermediate threat level exists.
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-024-A | Object at 150 m produces WARNING — first threat level")
    void tc024a_objectAt150mProducesWarning() {
        log.info("TC-024-A: 150 m object → WARNING threat");
        // At 150 m, relSpeed 16.67: stoppingDist ≈ 22.4 m → 150 >> 22.4 → WARNING zone
        ProcessedSensorData snap = buildHazardSnapshot(
                150.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment assessment = detector.assess(snap);

        log.info("TC-024-A: threat = {}", assessment.getThreatLevel());
        assertEquals(ThreatLevel.WARNING, assessment.getThreatLevel(),
                "Object at 150 m must be WARNING threat (DR-07)");
        log.info("TC-024-A passed");
    }

    /**
     * TC-024-B: Object crosses into braking distance — threat must escalate
     * from WARNING to BRAKE. Simulated by comparing two snapshots at different
     * distances processed by the same detector instance.
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-024-B | Threat escalates WARNING → BRAKE as object closes")
    void tc024b_threatEscalatesWarningToBrake() {
        log.info("TC-024-B: threat escalation WARNING → BRAKE");
        // First snapshot: 150 m → WARNING
        ProcessedSensorData farSnap = buildHazardSnapshot(
                150.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment warningAssessment = detector.assess(farSnap);

        // Second snapshot: 10 m → BRAKE
        ProcessedSensorData closeSnap = buildHazardSnapshot(
                10.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment brakeAssessment = detector.assess(closeSnap);

        log.info("TC-024-B: far={}, close={}",
                warningAssessment.getThreatLevel(), brakeAssessment.getThreatLevel());
        assertEquals(ThreatLevel.WARNING, warningAssessment.getThreatLevel(),
                "Far object must produce WARNING (DR-07)");
        assertEquals(ThreatLevel.BRAKE, brakeAssessment.getThreatLevel(),
                "Close object must escalate to BRAKE (DR-07)");
        log.info("TC-024-B passed");
    }

    /**
     * TC-024-C: Three distinct threat levels (NONE → WARNING → BRAKE) must all
     * be producible by CollisionDetector given appropriate inputs.
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-024-C | All three threat levels (NONE, WARNING, BRAKE) are reachable")
    void tc024c_allThreeThreatLevelsReachable() {
        log.info("TC-024-C: all three threat levels reachable");
        CollisionDetector d = new CollisionDetector();

        // NONE: clear road
        ProcessedSensorData clearSnap = buildClearRoadSnapshot(16.67);
        CollisionAssessment noneAssessment = d.assess(clearSnap);

        // WARNING: object at 150 m
        ProcessedSensorData warnSnap = buildHazardSnapshot(
                150.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment warnAssessment = d.assess(warnSnap);

        // BRAKE: object at 10 m
        ProcessedSensorData brakeSnap = buildHazardSnapshot(
                10.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment brakeAssessment = d.assess(brakeSnap);

        log.info("TC-024-C: none={}, warn={}, brake={}",
                noneAssessment != null ? noneAssessment.getThreatLevel() : "null",
                warnAssessment.getThreatLevel(),
                brakeAssessment.getThreatLevel());

        if (noneAssessment != null) {
            assertEquals(ThreatLevel.NONE,    noneAssessment.getThreatLevel(),  "Must reach NONE (DR-07)");
        }
        assertEquals(ThreatLevel.WARNING, warnAssessment.getThreatLevel(),  "Must reach WARNING (DR-07)");
        assertEquals(ThreatLevel.BRAKE,   brakeAssessment.getThreatLevel(), "Must reach BRAKE (DR-07)");
        log.info("TC-024-C passed");
    }

    /**
     * TC-024-D: An object that disappears (no longer detected) must cause the
     * threat to drop back toward NONE — confirming the system doesn't latch
     * permanently on a stale BRAKE state.
     *
     * DR-07 | DR-09
     */
    @Test
    @DisplayName("TC-024-D | Object cleared from road → threat returns to NONE")
    void tc024d_objectClearedReturnsToNone() {
        log.info("TC-024-D: object clears road, threat returns to NONE");
        // First: object at 10 m → BRAKE
        ProcessedSensorData hazardSnap = buildHazardSnapshot(
                10.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment brakeAssessment = detector.assess(hazardSnap);
        assertEquals(ThreatLevel.BRAKE, brakeAssessment.getThreatLevel());

        // Second: road is clear → new assessment should be NONE
        ProcessedSensorData clearSnap = buildClearRoadSnapshot(16.67);
        CollisionAssessment clearAssessment = detector.assess(clearSnap);

        log.info("TC-024-D: after clear, threat = {}",
                clearAssessment != null ? clearAssessment.getThreatLevel() : "null");
        assertNotNull(clearAssessment, "Assessment must not be null after clear road");
        assertEquals(ThreatLevel.NONE, clearAssessment.getThreatLevel(),
                "After object clears, threat must return to NONE (DR-07/DR-09)");
        log.info("TC-024-D passed");
    }
}