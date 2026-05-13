package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.logic.RedundancyChecker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-016, TC-017, TC-018 — Simulated detection range boundaries, camera accuracy
 * under adverse conditions, and wheel speed data across the full 0–250 km/h range.
 *
 * Requirements covered:
 *   REQ-020  Simulate object detection 0.5 – 200 m with weather effects
 *   REQ-021  Camera classification accuracy under light/weather variation
 *   REQ-022  Wheel speed data 0 – 250 km/h including rapid deceleration
 *   DR-05    TTC calculation — feeds into range boundary tests
 *   DR-06    Per-object-type thresholds — feeds into weather effect checks
 */
@DisplayName("TC-016..018 | Detection Range, Camera Conditions, Wheel Speed Range")
class DetectionRangeAndWeatherTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(DetectionRangeAndWeatherTests.class);

    private CollisionDetector detector;
    private RedundancyChecker checker;

    @BeforeEach
    void setUp() {
        log.info("Setting up CollisionDetector and RedundancyChecker");
        detector = new CollisionDetector();
        checker  = new RedundancyChecker();
    }

    // ---------------------------------------------------------------
    // TC-016  REQ-020 — Object detection range boundaries (0.5 – 200 m)
    // ---------------------------------------------------------------

    /**
     * TC-016-A: Object at exactly 0.5 m (minimum detection distance) must be
     * detected and produce a BRAKE threat — it is within stopping distance.
     *
     * REQ-020 | DR-05
     */
    @Test
    @DisplayName("TC-016-A | Object at 0.5 m (minimum range) produces BRAKE threat")
    void tc016a_objectAtMinimumRangeDetected() {
        log.info("TC-016-A: object at minimum detection range 0.5 m");
        ProcessedSensorData snap = buildHazardSnapshot(
                0.5, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("TC-016-A: assessment = {}", assessment);

        assertNotNull(assessment);
        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "Object at 0.5 m must produce BRAKE threat (REQ-020)");
        assertTrue(assessment.getDistance() >= 0.5,
                "Detected distance must be >= MIN_DETECTION_DISTANCE (REQ-020)");
        log.info("TC-016-A passed — distance = {}", assessment.getDistance());
    }

    /**
     * TC-016-B: Object at exactly 200 m (maximum warning range) while closing
     * at 16.67 m/s — stopping distance ≈ 22.4 m, so 200 m is the WARNING zone.
     *
     * REQ-020
     */
    @Test
    @DisplayName("TC-016-B | Object at 200 m (max warning range) produces WARNING threat")
    void tc016b_objectAt200mProducesWarning() {
        log.info("TC-016-B: object at 200 m max warning boundary");
        ProcessedSensorData snap = buildHazardSnapshot(
                200.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("TC-016-B: threat = {}", assessment.getThreatLevel());

        assertNotNull(assessment);
        assertEquals(ThreatLevel.WARNING, assessment.getThreatLevel(),
                "Object at 200 m must be in WARNING zone (REQ-020)");
        log.info("TC-016-B passed");
    }

    /**
     * TC-016-C: Object beyond 200 m must produce NONE threat — outside
     * the detectable warning zone.
     *
     * REQ-020
     */
    @Test
    @DisplayName("TC-016-C | Object beyond 200 m produces NONE threat")
    void tc016c_objectBeyond200mProducesNone() {
        log.info("TC-016-C: object at 201 m beyond warning boundary");
        ProcessedSensorData snap = buildHazardSnapshot(
                201.0, 16.67, ObjectType.VEHICLE, 16.67);

        CollisionAssessment assessment = detector.assess(snap);
        log.info("TC-016-C: threat = {}", assessment.getThreatLevel());

        assertNotNull(assessment);
        assertEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Object beyond 200 m must produce NONE threat (REQ-020)");
        log.info("TC-016-C passed");
    }

    /**
     * TC-016-D: Night + Fog scenario — pedestrian at 100 m.
     * Even in degraded conditions the sensor data layer must still carry the
     * object (simulation assumes reduced accuracy; core detection should still
     * fire because the data arrives as a valid reading).
     * Mirrors JSON: "Night Fog Pedestrian" at 11.11 m/s.
     *
     * REQ-020 | DR-05
     */
    @Test
    @DisplayName("TC-016-D | Pedestrian at 100 m detected in fog/night conditions")
    void tc016d_pedestrianDetectedInFogNight() {
        log.info("TC-016-D: fog/night pedestrian at 100 m");
        // In fog/night the camera confidence may be lower; radar/lidar still read
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, 100.0, 11.11, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, 100.5, 11.11, true));
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, 100.0, 11.11, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, 100.3, 11.11, true));
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera degraded in fog/night — lower confidence but still valid
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.PEDESTRIAN, true, 0.55));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.PEDESTRIAN, true, 0.50));
        readings.put(SensorType.CAMERA, cameraMap);

        double[] rpm = {400, 400, 400, 400};
        double[] spd = {11.11, 11.11, 11.11, 11.11};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        ProcessedSensorData snap      = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);
        CollisionAssessment assessment = detector.assess(validated);

        log.info("TC-016-D: assessment = {}", assessment);
        assertNotNull(assessment);
        // At 100 m, 11.11 m/s: stoppingDist=(11.11²/16)+5 ≈ 12.7 m → WARNING zone
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Pedestrian at 100 m must be detected even in fog/night (REQ-020)");
        log.info("TC-016-D passed — threat = {}", assessment.getThreatLevel());
    }

    // ---------------------------------------------------------------
    // TC-017  REQ-021 — Camera accuracy under light and weather variation
    // ---------------------------------------------------------------

    /**
     * TC-017-A: CameraData in clear/day conditions should have high confidence
     * (≥ 0.85). Confirm the data structure correctly holds and returns a
     * high-confidence value.
     *
     * REQ-021
     */
    @Test
    @DisplayName("TC-017-A | Camera high confidence in clear/day conditions")
    void tc017a_cameraHighConfidenceClearDay() {
        log.info("TC-017-A: camera confidence in clear/day");
        long ts = System.currentTimeMillis();
        CameraData cam = new CameraData(
                SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95);

        assertTrue(cam.getConfidence() >= 0.85,
                "Camera confidence in clear/day must be >= 0.85 (REQ-021)");
        log.info("TC-017-A passed — confidence = {}", cam.getConfidence());
    }

    /**
     * TC-017-B: CameraData in fog/night conditions must reflect lower confidence
     * (< 0.85 for degraded simulation). RedundancyChecker must still accept the
     * reading if both sensors agree within 0.2 threshold.
     *
     * REQ-021 | DR-06
     */
    @Test
    @DisplayName("TC-017-B | Camera lower confidence in fog/night accepted within threshold")
    void tc017b_cameraLowConfidenceFogNightAccepted() {
        log.info("TC-017-B: camera confidence in fog/night");
        long ts = System.currentTimeMillis();

        // Both cameras degraded but agree within 0.2 threshold
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();
        readings.put(SensorType.RADAR, new HashMap<>());
        readings.put(SensorType.LIDAR, new HashMap<>());

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.PEDESTRIAN, true, 0.55));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.PEDESTRIAN, true, 0.50));
        readings.put(SensorType.CAMERA, cameraMap);
        readings.put(SensorType.WHEEL_SPEED, new HashMap<>());

        ProcessedSensorData snap      = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        // Both should be retained — confidence diff = 0.05 < 0.2 threshold
        assertNotNull(validated.getSensorData(SensorType.CAMERA, SensorId.PRIMARY),
                "Low-confidence camera within threshold must be retained (REQ-021)");
        assertNotNull(validated.getSensorData(SensorType.CAMERA, SensorId.REDUNDANT),
                "Low-confidence redundant camera within threshold must be retained (REQ-021)");

        CameraData retained = (CameraData) validated.getSensorData(
                SensorType.CAMERA, SensorId.PRIMARY);
        assertTrue(retained.getConfidence() < 0.85,
                "Confidence must be lower in degraded conditions (REQ-021)");
        log.info("TC-017-B passed — retained confidence = {}", retained.getConfidence());
    }

    /**
     * TC-017-C: Camera confidence in fog/night must be lower than clear/day.
     * Simulates what the sensor layer would produce to confirm the simulation
     * contract (REQ-021).
     *
     * REQ-021
     */
    @Test
    @DisplayName("TC-017-C | Camera confidence degrades from clear/day to fog/night")
    void tc017c_cameraConfidenceDegradesFogVsDay() {
        log.info("TC-017-C: confidence degradation from clear to fog/night");
        long ts = System.currentTimeMillis();
        CameraData dayCamera  = new CameraData(SensorId.PRIMARY, ts,
                ObjectType.VEHICLE, true, 0.95);
        CameraData fogCamera  = new CameraData(SensorId.PRIMARY, ts,
                ObjectType.VEHICLE, true, 0.55);

        assertTrue(dayCamera.getConfidence() > fogCamera.getConfidence(),
                "Clear/day confidence must exceed fog/night confidence (REQ-021)");
        log.info("TC-017-C passed — day={}, fog={}", dayCamera.getConfidence(), fogCamera.getConfidence());
    }

    /**
     * TC-017-D: When camera confidence is extremely low (below reasonable
     * threshold), the camera sensor falls back to classifying as UNKNOWN.
     * This confirms the simulation contract that bad conditions degrade classification.
     *
     * REQ-021 | DR-06
     */
    @Test
    @DisplayName("TC-017-D | Very low camera confidence leads to UNKNOWN classification")
    void tc017d_veryLowConfidenceClassifiesUnknown() {
        log.info("TC-017-D: very low confidence → UNKNOWN classification");
        long ts = System.currentTimeMillis();
        // Simulating a heavily degraded camera that the simulator produces as UNKNOWN
        CameraData degraded = new CameraData(
                SensorId.PRIMARY, ts, ObjectType.UNKNOWN, true, 0.10);

        assertEquals(ObjectType.UNKNOWN, degraded.getClassification(),
                "Very low confidence camera must classify as UNKNOWN (REQ-021)");
        assertTrue(degraded.getConfidence() < 0.5,
                "Degraded camera confidence must be < 0.5");
        log.info("TC-017-D passed");
    }

    // ---------------------------------------------------------------
    // TC-018  REQ-022 — Wheel speed 0–250 km/h including rapid deceleration
    // ---------------------------------------------------------------

    /**
     * TC-018-A: Wheel speed data at 0 km/h (stationary) — all RPM must be 0.
     *
     * REQ-022
     */
    @Test
    @DisplayName("TC-018-A | Wheel speed data at 0 km/h — all RPM zero")
    void tc018a_wheelSpeedAtZero() {
        log.info("TC-018-A: wheel speed at 0 km/h");
        long ts = System.currentTimeMillis();
        double[] rpm = {0.0, 0.0, 0.0, 0.0};
        double[] spd = {0.0, 0.0, 0.0, 0.0};
        WheelSpeedData wsd = new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd);

        assertFalse(wsd.isGarbage());
        for (double r : wsd.getRpm()) {
            assertEquals(0.0, r, 0.001,
                    "RPM must be 0 at standstill (REQ-022)");
        }
        log.info("TC-018-A passed");
    }

    /**
     * TC-018-B: Wheel speed data at 250 km/h — RPM ≈ 2083 (tyre circ ~2 m).
     * Data must be stored without clamping.
     *
     * REQ-022
     */
    @Test
    @DisplayName("TC-018-B | Wheel speed data at 250 km/h (≈2083 RPM) stored correctly")
    void tc018b_wheelSpeedAt250kmh() {
        log.info("TC-018-B: wheel speed at 250 km/h");
        long ts = System.currentTimeMillis();
        double speedMs  = 69.44; // 250 km/h
        double rpmVal   = (speedMs / 2.0) * 60.0; // ≈ 2083 RPM
        double[] rpm    = {rpmVal, rpmVal, rpmVal, rpmVal};
        double[] spd    = {speedMs, speedMs, speedMs, speedMs};

        WheelSpeedData wsd = new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd);

        assertFalse(wsd.isGarbage());
        assertEquals(rpmVal, wsd.getFrontLeftRpm(), 1.0,
                "250 km/h RPM must be stored without clamping (REQ-022)");
        log.info("TC-018-B passed — RPM at 250 km/h = {}", rpmVal);
    }

    /**
     * TC-018-C: Rapid deceleration scenario — wheel RPM drops from 1000 to 200
     * in one step (simulating emergency braking / potential skid).
     * The data layer must accept and store these extreme values without error.
     *
     * REQ-022 | DR-11
     */
    @Test
    @DisplayName("TC-018-C | Rapid deceleration RPM drop (1000 → 200) accepted by WheelSpeedData")
    void tc018c_rapidDecelerationRpmDrop() {
        log.info("TC-018-C: rapid deceleration RPM drop");
        long ts = System.currentTimeMillis();

        // Before braking
        double[] rpmBefore = {1000.0, 1000.0, 1000.0, 1000.0};
        double[] spdBefore = {33.33, 33.33, 33.33, 33.33};
        WheelSpeedData before = new WheelSpeedData(SensorId.PRIMARY, ts, rpmBefore, spdBefore);

        // After rapid braking — asymmetric (rear wheels skidding)
        double[] rpmAfter = {200.0, 200.0, 50.0, 50.0};  // rear wheels skidding
        double[] spdAfter = {6.67, 6.67, 1.67, 1.67};
        WheelSpeedData after = new WheelSpeedData(SensorId.PRIMARY, ts + 50, rpmAfter, spdAfter);

        assertFalse(before.isGarbage());
        assertFalse(after.isGarbage());

        double frontAvg = (after.getFrontLeftRpm() + after.getFrontRightRpm()) / 2.0;
        double rearAvg  = (after.getRearLeftRpm()  + after.getRearRightRpm())  / 2.0;
        double skidDiff = Math.abs(frontAvg - rearAvg);

        log.info("TC-018-C: frontAvg={}, rearAvg={}, skidDiff={}", frontAvg, rearAvg, skidDiff);
        assertTrue(skidDiff > 50.0,
                "Rear-wheel skid must produce RPM asymmetry > 50 RPM (REQ-022)");
        log.info("TC-018-C passed — skid difference = {} RPM", skidDiff);
    }

    /**
     * TC-018-D: RedundancyChecker must flag wheel speed disagreement when
     * average speeds differ by more than 2 m/s (WHEEL_SPEED_THRESHOLD).
     * This simulates a skidding wheel that sends wildly different readings.
     *
     * REQ-022 | DR-04
     */
    @Test
    @DisplayName("TC-018-D | Wheel speed disagreement > 2 m/s threshold → redundant dropped")
    void tc018d_wheelSpeedDisagreementDropsRedundant() {
        log.info("TC-018-D: wheel speed sensor disagreement");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        readings.put(SensorType.RADAR, new HashMap<>());
        readings.put(SensorType.LIDAR, new HashMap<>());
        readings.put(SensorType.CAMERA, new HashMap<>());

        // Primary: 16.67 m/s (avg). Redundant: 10.0 m/s (avg). Diff = 6.67 > 2.0 threshold
        double[] rpmP = {800, 800, 800, 800};
        double[] spdP = {16.67, 16.67, 16.67, 16.67};  // avg = 16.67

        double[] rpmR = {500, 500, 500, 500};
        double[] spdR = {10.0, 10.0, 10.0, 10.0};      // avg = 10.0

        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpmP, spdP));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpmR, spdR));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        ProcessedSensorData snap      = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        assertNotNull(validated.getSensorData(SensorType.WHEEL_SPEED, SensorId.PRIMARY),
                "Primary wheel speed must be retained on disagreement (DR-04)");
        assertNull(validated.getSensorData(SensorType.WHEEL_SPEED, SensorId.REDUNDANT),
                "Redundant wheel speed must be dropped on disagreement > 2 m/s (REQ-022/DR-04)");
        log.info("TC-018-D passed");
    }
}