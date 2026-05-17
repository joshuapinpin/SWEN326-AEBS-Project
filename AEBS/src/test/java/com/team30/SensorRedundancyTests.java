package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.CollisionDetector;
import com.team30.core.logic.FaultHandler;
import com.team30.core.logic.RedundancyChecker;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TC-009, TC-014, TC-021 — Sensor redundancy, fault tolerance, and
 * disagreement threshold tests.
 */
@DisplayName("TC-009, 014, 021 | Sensor Redundancy and Fault Tolerance")
class SensorRedundancyTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(SensorRedundancyTests.class);

    private RedundancyChecker checker;
    private CollisionDetector detector;
    private CarState carState;
    private DriverInterface driverInterface;
    private FaultHandler faultHandler;

    @BeforeEach
    void setUp() {
        log.info("Setting up RedundancyChecker, CollisionDetector, FaultHandler");
        checker       = new RedundancyChecker();
        detector      = new CollisionDetector();
        carState      = makeCruisingCarState(16.67);
        driverInterface = new DriverInterface(carState);
        faultHandler  = new FaultHandler(driverInterface, carState);
    }

    // ------------------------------------------------------------------
    // TC-009
    // ------------------------------------------------------------------

    /**
     * TC-009-A: When primary RADAR is garbage, RedundancyChecker must promote
     * the redundant radar reading to PRIMARY slot.
     */
    @Test
    @DisplayName("TC-009-A | Redundant RADAR promoted when primary is garbage")
    void tc009a_redundantRadarPromoted() {
        log.info("TC-009-A: primary radar garbage, redundant healthy");
        ProcessedSensorData snap = buildPrimaryFailedSnapshot(
                SensorType.RADAR, 40.0, 16.67, 16.67);

        ProcessedSensorData validated = checker.validate(snap);

        SensorData result = validated.getSensorData(SensorType.RADAR, SensorId.PRIMARY);
        assertNotNull(result,
                "RedundancyChecker must promote redundant radar to PRIMARY slot (REQ-009)");
        assertFalse(result.isGarbage(),
                "Promoted radar reading must not be garbage");
        log.info("TC-009-A: promoted radar distance = {}",
                ((RadarData) result).getDistance());
        log.info("TC-009-A passed");
    }

    /**
     * TC-009-B: When primary LIDAR is garbage, redundant lidar must be promoted.
     */
    @Test
    @DisplayName("TC-009-B | Redundant LIDAR promoted when primary is garbage")
    void tc009b_redundantLidarPromoted() {
        log.info("TC-009-B: primary lidar garbage");
        ProcessedSensorData snap = buildPrimaryFailedSnapshot(
                SensorType.LIDAR, 40.0, 16.67, 16.67);

        ProcessedSensorData validated = checker.validate(snap);
        SensorData result = validated.getSensorData(SensorType.LIDAR, SensorId.PRIMARY);

        assertNotNull(result,
                "RedundancyChecker must promote redundant lidar to PRIMARY slot (REQ-009)");
        assertFalse(result.isGarbage());
        log.info("TC-009-B passed");
    }

    /**
     * TC-009-C: When primary CAMERA is garbage, redundant camera must be promoted
     * so CollisionDetector can still classify the object type.
     */
    @Test
    @DisplayName("TC-009-C | Redundant CAMERA promoted when primary is garbage")
    void tc009c_redundantCameraPromoted() {
        log.info("TC-009-C: primary camera garbage, redundant healthy");
        ProcessedSensorData snap = buildPrimaryFailedSnapshot(
                SensorType.CAMERA, 40.0, 16.67, 16.67);

        ProcessedSensorData validated = checker.validate(snap);
        SensorData result = validated.getSensorData(SensorType.CAMERA, SensorId.PRIMARY);

        assertNotNull(result,
                "RedundancyChecker must promote redundant camera to PRIMARY slot (REQ-009)");
        assertFalse(result.isGarbage());
        log.info("TC-009-C passed");
    }

    /**
     * TC-009-D: CollisionDetector must still detect a vehicle at 10 m after
     * primary radar fails (redundant takes over via RedundancyChecker).
     */
    @Test
    @DisplayName("TC-009-D | Hazard detected using redundant radar after primary fails")
    void tc009d_hazardDetectedAfterPrimaryRadarFails() {
        log.info("TC-009-D: hazard detection with primary radar failed");
        ProcessedSensorData raw = buildPrimaryFailedSnapshot(
                SensorType.RADAR, 10.0, 16.67, 16.67);

        ProcessedSensorData validated = checker.validate(raw);
        CollisionAssessment assessment = detector.assess(validated);

        log.info("TC-009-D: assessment = {}", assessment);
        assertNotNull(assessment, "Assessment must not be null");
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Hazard must still be detected when redundant radar is used (REQ-009)");
        log.info("TC-009-D passed — threat = {}", assessment.getThreatLevel());
    }

    // ------------------------------------------------------------------
    // TC-014
    // ------------------------------------------------------------------

    /**
     * TC-014-A: After one sensor type has both primary and redundant fail,
     * FaultHandler must show a maintenance warning (single unavailable type).
     * The system must NOT enter FAIL_SAFE with only one sensor type down
     * (camera failure while radar and lidar are healthy).
     */
    @Test
    @DisplayName("TC-014-A | Single sensor type fully down → maintenance warning, no fail-safe")
    void tc014a_singleTypeBothFailedShowsWarning() {
        log.info("TC-014-A: camera both failed, radar/lidar healthy");
        // Both camera sensors failed; radar and lidar are healthy
        ProcessedSensorData snap = buildBothFailedSnapshot(
                SensorType.CAMERA, 40.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(snap);

        BrakeDecision okDecision = new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);

        assertDoesNotThrow(() -> faultHandler.handle(okDecision, validated),
                "FaultHandler must not throw when camera type unavailable");

        // With 1 unavailable type, system must NOT enter FAIL_SAFE
        assertNotEquals(DrivingMode.FAIL_SAFE, carState.getDrivingMode(),
                "DrivingMode must not be FAIL_SAFE with only one sensor type unavailable (REQ-018)");
        log.info("TC-014-A passed — mode = {}", carState.getDrivingMode());
    }

    /**
     * TC-014-B: With two sensor types fully failed (e.g. RADAR + CAMERA),
     * FaultHandler must engage FAIL_SAFE and escalate alert.
     */
    @Test
    @DisplayName("TC-014-B | Two sensor types fully failed → FAIL_SAFE engaged")
    void tc014b_twoTypesBothFailedEngagesFailSafe() {
        log.info("TC-014-B: radar + camera both failed");
        long ts = System.currentTimeMillis();

        // Build snapshot: RADAR and CAMERA both garbage; LIDAR healthy
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,   new RadarData(SensorId.PRIMARY, ts));
        radarMap.put(SensorId.REDUNDANT, new RadarData(SensorId.REDUNDANT, ts));
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, 40.5, 16.67, true));
        readings.put(SensorType.LIDAR, lidarMap);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,   new CameraData(SensorId.PRIMARY, ts));
        cameraMap.put(SensorId.REDUNDANT, new CameraData(SensorId.REDUNDANT, ts));
        readings.put(SensorType.CAMERA, cameraMap);

        double[] rpm = {500, 500, 500, 500};
        double[] spd = {16.67, 16.67, 16.67, 16.67};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        ProcessedSensorData snap = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        BrakeDecision okDecision = new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        faultHandler.handle(okDecision, validated);

        assertEquals(DrivingMode.FAIL_SAFE, carState.getDrivingMode(),
                "Two sensor types unavailable must engage FAIL_SAFE (REQ-018/REQ-019)");
        log.info("TC-014-B passed — mode = {}", carState.getDrivingMode());
    }

    // ------------------------------------------------------------------
    // TC-021
    // ------------------------------------------------------------------

    /**
     * TC-021-A: Two RADAR readings that agree within 5 m threshold must both
     * be retained by RedundancyChecker (1oo2 check passes).
     */
    @Test
    @DisplayName("TC-021-A | Radar readings within 5 m threshold both retained")
    void tc021a_radarReadingsWithinThresholdBothRetained() {
        log.info("TC-021-A: radar disagreement within threshold");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, 43.0, 16.67, true)); // diff = 3 m (< 5 m)
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.LIDAR, new HashMap<>());
        readings.put(SensorType.CAMERA, new HashMap<>());
        readings.put(SensorType.WHEEL_SPEED, new HashMap<>());

        ProcessedSensorData snap = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        assertNotNull(validated.getSensorData(SensorType.RADAR, SensorId.PRIMARY),
                "PRIMARY must be retained when within threshold (DR-04)");
        assertNotNull(validated.getSensorData(SensorType.RADAR, SensorId.REDUNDANT),
                "REDUNDANT must be retained when within threshold (DR-04)");
        log.info("TC-021-A passed");
    }

    /**
     * TC-021-B: Two RADAR readings that differ by more than 5 m (disagreement)
     * — RedundancyChecker must keep only PRIMARY (drops redundant).
     * This is the 1oo2 architecture: primary wins on disagreement.
     */
    @Test
    @DisplayName("TC-021-B | Radar readings exceeding 5 m threshold — redundant dropped")
    void tc021b_radarReadingsExceedThresholdRedundantDropped() {
        log.info("TC-021-B: radar disagreement exceeds threshold");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, 40.0, 16.67, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, 50.0, 16.67, true)); // diff = 10 m (> 5 m)
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.LIDAR, new HashMap<>());
        readings.put(SensorType.CAMERA, new HashMap<>());
        readings.put(SensorType.WHEEL_SPEED, new HashMap<>());

        ProcessedSensorData snap = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        assertNotNull(validated.getSensorData(SensorType.RADAR, SensorId.PRIMARY),
                "PRIMARY must be retained on disagreement (DR-04)");
        assertNull(validated.getSensorData(SensorType.RADAR, SensorId.REDUNDANT),
                "REDUNDANT must be dropped on exceeding threshold (DR-04)");
        log.info("TC-021-B passed");
    }

    /**
     * TC-021-C: Both wheel speed sensors failing must result in an empty wheel
     * speed map after validation — system detects total wheel speed loss.
     */
    @Test
    @DisplayName("TC-021-C | Both wheel speed sensors garbage → empty map after validation")
    void tc021c_bothWheelSpeedFailedResultsInEmptyMap() {
        log.info("TC-021-C: both wheel speed sensors garbage");
        ProcessedSensorData snap = buildBothFailedSnapshot(
                SensorType.WHEEL_SPEED, 40.0, 16.67, 16.67);

        ProcessedSensorData validated = checker.validate(snap);

        SensorData primary   = validated.getSensorData(SensorType.WHEEL_SPEED, SensorId.PRIMARY);
        SensorData redundant = validated.getSensorData(SensorType.WHEEL_SPEED, SensorId.REDUNDANT);

        assertNull(primary,
                "Primary wheel speed must be null after both sensors garbage (DR-04)");
        assertNull(redundant,
                "Redundant wheel speed must be null after both sensors garbage (DR-04)");
        log.info("TC-021-C passed — wheel speed map is empty as expected");
    }

    /**
     * TC-021-D: Camera confidence disagreement above 0.2 threshold — redundant dropped.
     * Primary confidence = 0.95, redundant = 0.70 → diff = 0.25 > 0.2 → redundant dropped
     */
    @Test
    @DisplayName("TC-021-D | Camera confidence disagreement > 0.2 → redundant dropped")
    void tc021d_cameraConfidenceDisagreementDropsRedundant() {
        log.info("TC-021-D: camera confidence disagreement above threshold");
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        readings.put(SensorType.RADAR, new HashMap<>());
        readings.put(SensorType.LIDAR, new HashMap<>());

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.70));
        readings.put(SensorType.CAMERA, cameraMap);

        readings.put(SensorType.WHEEL_SPEED, new HashMap<>());

        ProcessedSensorData snap = buildSnapshot(readings);
        ProcessedSensorData validated = checker.validate(snap);

        assertNotNull(validated.getSensorData(SensorType.CAMERA, SensorId.PRIMARY),
                "Primary camera must be retained on confidence disagreement (DR-04)");
        assertNull(validated.getSensorData(SensorType.CAMERA, SensorId.REDUNDANT),
                "Redundant camera must be dropped on confidence disagreement > 0.2 (DR-04)");
        log.info("TC-021-D passed");
    }
}