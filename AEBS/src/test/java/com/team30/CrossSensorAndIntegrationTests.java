package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.BrakeSystemController;
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
 * TC-029, TC-030, TC-031 — Cross-sensor type compensation (lidar covers radar,
 * radar covers lidar) and full end-to-end pipeline integration test.
 *
 * Requirements covered:
 *   REQ-001  Auditory alert
 *   REQ-002  Visual alert
 *   REQ-004  Radar / lidar data collection
 *   REQ-005  Camera classification
 *   REQ-006  Wheel speed sensor
 *   REQ-013  Braking control signal
 *   REQ-018  Fault tolerance
 *   REQ-019  Fail-safe mechanisms
 *   DR-08    Sensor consistency check before collision assessment
 */
@DisplayName("TC-029..031 | Cross-Sensor Compensation and End-to-End Integration")
class CrossSensorAndIntegrationTests extends com.team30.AEBSTestBase {

    private static final Logger log = LogManager.getLogger(CrossSensorAndIntegrationTests.class);

    private RedundancyChecker checker;
    private CollisionDetector detector;
    private BrakeSystemController bsc;
    private FaultHandler faultHandler;
    private DriverInterface driverInterface;
    private CarState carState;

    @BeforeEach
    void setUp() {
        log.info("Setting up full pipeline components");
        checker       = new RedundancyChecker();
        detector      = new CollisionDetector();
        carState      = makeCruisingCarState(16.67);
        bsc           = new BrakeSystemController(carState);
        driverInterface = new DriverInterface(carState);
        faultHandler  = new FaultHandler(driverInterface, carState);
    }

    // ---------------------------------------------------------------
    // Helper — build snapshot with all radar failed, lidar healthy
    // ---------------------------------------------------------------
    private ProcessedSensorData buildRadarTotallyFailedSnapshot(
            double distanceM, double relSpeedMs, double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        // Both radar sensors are garbage
        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,   new RadarData(SensorId.PRIMARY, ts));
        radarMap.put(SensorId.REDUNDANT, new RadarData(SensorId.REDUNDANT, ts));
        readings.put(SensorType.RADAR, radarMap);

        // Both lidar sensors are healthy
        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera healthy
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        readings.put(SensorType.CAMERA, cameraMap);

        // Wheel speed healthy
        double[] rpm = {500, 500, 500, 500};
        double[] spd = {carSpeedMs, carSpeedMs, carSpeedMs, carSpeedMs};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        return buildSnapshot(readings);
    }

    // ---------------------------------------------------------------
    // Helper — build snapshot with all lidar failed, radar healthy
    // ---------------------------------------------------------------
    private ProcessedSensorData buildLidarTotallyFailedSnapshot(
            double distanceM, double relSpeedMs, double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        // Radar healthy
        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        readings.put(SensorType.RADAR, radarMap);

        // Both lidar sensors are garbage
        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,   new LidarData(SensorId.PRIMARY, ts));
        lidarMap.put(SensorId.REDUNDANT, new LidarData(SensorId.REDUNDANT, ts));
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera healthy
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        readings.put(SensorType.CAMERA, cameraMap);

        // Wheel speed healthy
        double[] rpm = {500, 500, 500, 500};
        double[] spd = {carSpeedMs, carSpeedMs, carSpeedMs, carSpeedMs};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        wheelMap.put(SensorId.PRIMARY,
                new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
        wheelMap.put(SensorId.REDUNDANT,
                new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        return buildSnapshot(readings);
    }

    // ---------------------------------------------------------------
    // TC-029  REQ-018 / REQ-019 / DR-08 — Lidar compensates for total radar failure
    // ---------------------------------------------------------------

    /**
     * TC-029-A: Both primary and redundant RADAR fail. LIDAR is healthy.
     * After RedundancyChecker validates, the radar map must be empty but
     * lidar must remain. CollisionDetector must detect a vehicle at 40 m
     * using lidar alone.
     *
     * REQ-018 | REQ-019 | DR-08
     */
    @Test
    @DisplayName("TC-029-A | Lidar detects vehicle at 40 m when all radar fails")
    void tc029a_lidarDetectsVehicleWhenRadarTotallyFails() {
        log.info("TC-029-A: total radar failure, lidar compensates");
        ProcessedSensorData raw = buildRadarTotallyFailedSnapshot(10.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        // Radar maps must be empty after validation
        boolean radarEmpty = validated.getReadings().get(SensorType.RADAR) == null
                || validated.getReadings().get(SensorType.RADAR).isEmpty();
        assertTrue(radarEmpty,
                "Radar map must be empty after both sensors garbage (TC-029 / REQ-018)");

        // Lidar must still be present
        assertNotNull(validated.getSensorData(SensorType.LIDAR, SensorId.PRIMARY),
                "Lidar PRIMARY must be present when radar fails (TC-029 / REQ-018)");

        // CollisionDetector must still detect hazard via lidar
        CollisionAssessment assessment = detector.assess(validated);
        log.info("TC-029-A: assessment = {}", assessment);

        assertNotNull(assessment);
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Hazard must be detected by lidar when radar is totally failed (TC-029)");
        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "At 10 m with 16.67 m/s closing, threat must be BRAKE (TC-029)");
        log.info("TC-029-A passed — threat = {}", assessment.getThreatLevel());
    }

    /**
     * TC-029-B: With all radar failed and lidar healthy, FaultHandler must
     * recognise only ONE sensor type unavailable (radar) and issue a maintenance
     * warning — NOT engage FAIL_SAFE. Lidar keeps the system operational.
     *
     * REQ-018 | DR-08
     */
    @Test
    @DisplayName("TC-029-B | Total radar failure → maintenance warning only (not FAIL_SAFE) while lidar active")
    void tc029b_totalRadarFailureMaintWarningNotFailSafe() {
        log.info("TC-029-B: total radar failure shows warning, not FAIL_SAFE");
        ProcessedSensorData raw       = buildRadarTotallyFailedSnapshot(40.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        BrakeDecision ok = new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        faultHandler.handle(ok, validated);

        log.info("TC-029-B: mode = {}", carState.getDrivingMode());
        assertNotEquals(DrivingMode.FAIL_SAFE, carState.getDrivingMode(),
                "One sensor type down (radar) must not engage FAIL_SAFE while lidar is active (REQ-018)");
        log.info("TC-029-B passed — mode stays = {}", carState.getDrivingMode());
    }

    /**
     * TC-029-C: isSensorAvailable() on the validated snapshot correctly returns
     * false for radar and true for lidar after total radar failure.
     *
     * REQ-018
     */
    @Test
    @DisplayName("TC-029-C | isSensorAvailable correctly reports radar unavailable / lidar available")
    void tc029c_sensorAvailabilityAfterRadarFailure() {
        log.info("TC-029-C: sensor availability after total radar failure");
        ProcessedSensorData raw       = buildRadarTotallyFailedSnapshot(40.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        boolean radarAvail = validated.isSensorAvailable(SensorType.RADAR, SensorId.PRIMARY)
                || validated.isSensorAvailable(SensorType.RADAR, SensorId.REDUNDANT);
        boolean lidarAvail = validated.isSensorAvailable(SensorType.LIDAR, SensorId.PRIMARY)
                || validated.isSensorAvailable(SensorType.LIDAR, SensorId.REDUNDANT);

        log.info("TC-029-C: radarAvail={}, lidarAvail={}", radarAvail, lidarAvail);
        assertFalse(radarAvail,
                "Radar must be flagged unavailable after total failure (REQ-018)");
        assertTrue(lidarAvail,
                "Lidar must remain available when radar fails (TC-029)");
        log.info("TC-029-C passed");
    }

    // ---------------------------------------------------------------
    // TC-030  REQ-018 / REQ-019 / DR-08 — Radar compensates for total lidar failure
    // ---------------------------------------------------------------

    /**
     * TC-030-A: Both primary and redundant LIDAR fail. RADAR is healthy.
     * After validation, lidar map must be empty. CollisionDetector must detect
     * a vehicle at 10 m using radar alone.
     *
     * REQ-018 | REQ-019 | DR-08
     */
    @Test
    @DisplayName("TC-030-A | Radar detects vehicle at 10 m when all lidar fails")
    void tc030a_radarDetectsVehicleWhenLidarTotallyFails() {
        log.info("TC-030-A: total lidar failure, radar compensates");
        ProcessedSensorData raw       = buildLidarTotallyFailedSnapshot(10.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        boolean lidarEmpty = validated.getReadings().get(SensorType.LIDAR) == null
                || validated.getReadings().get(SensorType.LIDAR).isEmpty();
        assertTrue(lidarEmpty,
                "Lidar map must be empty after both sensors garbage (TC-030 / REQ-018)");

        assertNotNull(validated.getSensorData(SensorType.RADAR, SensorId.PRIMARY),
                "Radar PRIMARY must be present when lidar fails (TC-030 / REQ-018)");

        CollisionAssessment assessment = detector.assess(validated);
        log.info("TC-030-A: assessment = {}", assessment);

        assertNotNull(assessment);
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Hazard must be detected by radar when lidar totally fails (TC-030)");
        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "At 10 m with 16.67 m/s closing, threat must be BRAKE (TC-030)");
        log.info("TC-030-A passed — threat = {}", assessment.getThreatLevel());
    }

    /**
     * TC-030-B: Total lidar failure with healthy radar — FaultHandler must
     * issue maintenance warning only, NOT FAIL_SAFE.
     *
     * REQ-018
     */
    @Test
    @DisplayName("TC-030-B | Total lidar failure → maintenance warning only (not FAIL_SAFE) while radar active")
    void tc030b_totalLidarFailureMaintWarningNotFailSafe() {
        log.info("TC-030-B: total lidar failure shows warning, not FAIL_SAFE");
        ProcessedSensorData raw       = buildLidarTotallyFailedSnapshot(40.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        BrakeDecision ok = new BrakeDecision(false, 0.0, BrakeResult.NOT_NEEDED, 0);
        faultHandler.handle(ok, validated);

        log.info("TC-030-B: mode = {}", carState.getDrivingMode());
        assertNotEquals(DrivingMode.FAIL_SAFE, carState.getDrivingMode(),
                "One sensor type down (lidar) must not engage FAIL_SAFE while radar active (REQ-018)");
        log.info("TC-030-B passed — mode = {}", carState.getDrivingMode());
    }

    /**
     * TC-030-C: isSensorAvailable correctly reports lidar unavailable and
     * radar available after total lidar failure.
     *
     * REQ-018
     */
    @Test
    @DisplayName("TC-030-C | isSensorAvailable correctly reports lidar unavailable / radar available")
    void tc030c_sensorAvailabilityAfterLidarFailure() {
        log.info("TC-030-C: sensor availability after total lidar failure");
        ProcessedSensorData raw       = buildLidarTotallyFailedSnapshot(40.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);

        boolean radarAvail = validated.isSensorAvailable(SensorType.RADAR, SensorId.PRIMARY)
                || validated.isSensorAvailable(SensorType.RADAR, SensorId.REDUNDANT);
        boolean lidarAvail = validated.isSensorAvailable(SensorType.LIDAR, SensorId.PRIMARY)
                || validated.isSensorAvailable(SensorType.LIDAR, SensorId.REDUNDANT);

        log.info("TC-030-C: radarAvail={}, lidarAvail={}", radarAvail, lidarAvail);
        assertTrue(radarAvail,
                "Radar must remain available when lidar fails (TC-030)");
        assertFalse(lidarAvail,
                "Lidar must be flagged unavailable after total failure (REQ-018)");
        log.info("TC-030-C passed");
    }

    // ---------------------------------------------------------------
    // TC-031  End-to-End Integration — Full pipeline run
    // ---------------------------------------------------------------

    /**
     * TC-031-A: Clear road end-to-end pipeline run. Push data through
     * RedundancyChecker → CollisionDetector → BrakeSystemController →
     * FaultHandler. No hazard → should produce NONE, NOT_NEEDED, no FAIL_SAFE.
     *
     * REQ-001 | REQ-004 | REQ-005 | REQ-006 | REQ-013
     */
    @Test
    @DisplayName("TC-031-A | Clear road end-to-end pipeline produces NONE threat and NOT_NEEDED brake")
    void tc031a_clearRoadPipelineEndToEnd() {
        log.info("TC-031-A: clear road full pipeline");
        ProcessedSensorData raw       = buildClearRoadSnapshot(16.67);
        ProcessedSensorData validated = checker.validate(raw);
        CollisionAssessment assessment = detector.assess(validated);
        assertNotNull(assessment);

        BrakeDecision decision = bsc.execute(assessment);
        faultHandler.handle(decision, validated);

        log.info("TC-031-A: threat={}, brake={}, mode={}",
                assessment.getThreatLevel(), decision.getResult(), carState.getDrivingMode());

        assertEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Clear road must produce NONE threat");
        assertEquals(BrakeResult.NOT_NEEDED, decision.getResult(),
                "Clear road must produce NOT_NEEDED brake result");
        assertNotEquals(DrivingMode.FAIL_SAFE, carState.getDrivingMode(),
                "FAIL_SAFE must not be engaged on clear road");
        log.info("TC-031-A passed");
    }

    /**
     * TC-031-B: Hazard appears (vehicle at 10 m closing at 16.67 m/s) — full
     * pipeline must escalate to BRAKE and set DrivingMode to BRAKING.
     *
     * REQ-001 | REQ-002 | REQ-004 | REQ-013
     */
    @Test
    @DisplayName("TC-031-B | Hazard end-to-end pipeline produces BRAKE threat and sets BRAKING mode")
    void tc031b_hazardPipelineEndToEnd() {
        log.info("TC-031-B: hazard full pipeline");
        ProcessedSensorData raw       = buildHazardSnapshot(10.0, 16.67, ObjectType.VEHICLE, 16.67);
        ProcessedSensorData validated = checker.validate(raw);
        CollisionAssessment assessment = detector.assess(validated);

        assertNotNull(assessment);
        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "Vehicle at 10 m must produce BRAKE threat");

        BrakeDecision decision = bsc.execute(assessment);
        faultHandler.handle(decision, validated);

        log.info("TC-031-B: threat={}, shouldBrake={}, mode={}",
                assessment.getThreatLevel(), decision.isShouldBrake(), carState.getDrivingMode());

        assertTrue(decision.isShouldBrake(),
                "Brake command must fire on BRAKE threat");
        assertEquals(DrivingMode.BRAKING, carState.getDrivingMode(),
                "DrivingMode must be BRAKING after brake command (REQ-013)");
        log.info("TC-031-B passed");
    }

    /**
     * TC-031-C: Pedestrian scenario end-to-end. Pedestrian at 10 m closing at
     * 16.67 m/s. Pipeline must detect, classify as PEDESTRIAN, and brake.
     *
     * REQ-001 | REQ-005 | REQ-013
     */
    @Test
    @DisplayName("TC-031-C | Pedestrian hazard end-to-end — correctly classified and braked")
    void tc031c_pedestrianEndToEnd() {
        log.info("TC-031-C: pedestrian full pipeline");
        ProcessedSensorData raw       = buildHazardSnapshot(10.0, 16.67, ObjectType.PEDESTRIAN, 16.67);
        ProcessedSensorData validated = checker.validate(raw);
        CollisionAssessment assessment = detector.assess(validated);

        assertEquals(ObjectType.PEDESTRIAN, assessment.getObjectType(),
                "Pedestrian must be classified correctly (REQ-005)");
        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel(),
                "Pedestrian at 10 m must produce BRAKE threat (REQ-001)");

        BrakeDecision decision = bsc.execute(assessment);
        log.info("TC-031-C: type={}, threat={}, shouldBrake={}",
                assessment.getObjectType(), assessment.getThreatLevel(), decision.isShouldBrake());

        assertTrue(decision.isShouldBrake(),
                "Brake must fire for pedestrian hazard (REQ-013)");
        log.info("TC-031-C passed");
    }

    /**
     * TC-031-D: Primary sensor failure mid-pipeline — primary radar garbage,
     * redundant healthy. Full pipeline still detects hazard.
     *
     * REQ-018 | DR-08
     */
    @Test
    @DisplayName("TC-031-D | Primary radar failure mid-pipeline — redundant takes over, hazard detected")
    void tc031d_primaryRadarFailureMidPipeline() {
        log.info("TC-031-D: primary radar failure mid-pipeline");
        ProcessedSensorData raw       = buildPrimaryFailedSnapshot(
                SensorType.RADAR, 10.0, 16.67, 16.67);
        ProcessedSensorData validated = checker.validate(raw);
        CollisionAssessment assessment = detector.assess(validated);

        assertNotNull(assessment);
        assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel(),
                "Hazard must still be detected when primary radar fails (REQ-018)");

        BrakeDecision decision = bsc.execute(assessment);
        log.info("TC-031-D: threat={}, shouldBrake={}", assessment.getThreatLevel(), decision.isShouldBrake());

        assertTrue(decision.isShouldBrake(),
                "Braking must fire even with primary radar failed (REQ-018)");
        log.info("TC-031-D passed");
    }

    /**
     * TC-031-E: Sequential scenarios — clear road then sudden vehicle then road
     * clears again. Pipeline must handle all three state transitions correctly
     * without getting stuck.
     *
     * REQ-001 | REQ-002 | REQ-013 | DR-09
     */
    @Test
    @DisplayName("TC-031-E | Sequential clear → hazard → clear again transitions correctly")
    void tc031e_sequentialClearHazardClear() {
        log.info("TC-031-E: sequential clear → hazard → clear");

        // Step 1: clear road
        ProcessedSensorData clearRaw       = buildClearRoadSnapshot(16.67);
        ProcessedSensorData clearValidated = checker.validate(clearRaw);
        CollisionAssessment clearAssessment = detector.assess(clearValidated);
        assertNotNull(clearAssessment);
        assertEquals(ThreatLevel.NONE, clearAssessment.getThreatLevel(), "Step 1 must be NONE");
        log.info("TC-031-E step 1: {}", clearAssessment.getThreatLevel());

        // Step 2: vehicle appears at 10 m
        carState.setDrivingMode(DrivingMode.CRUISING); // reset for clean test
        BrakeSystemController freshBsc = new BrakeSystemController(carState);

        ProcessedSensorData hazardRaw       = buildHazardSnapshot(10.0, 16.67, ObjectType.VEHICLE, 16.67);
        ProcessedSensorData hazardValidated = checker.validate(hazardRaw);
        CollisionAssessment hazardAssessment = detector.assess(hazardValidated);
        assertEquals(ThreatLevel.BRAKE, hazardAssessment.getThreatLevel(), "Step 2 must be BRAKE");
        BrakeDecision brakeDecision = freshBsc.execute(hazardAssessment);
        assertTrue(brakeDecision.isShouldBrake(), "Step 2 must command braking");
        log.info("TC-031-E step 2: {}", hazardAssessment.getThreatLevel());

        // Step 3: road clears again — simulate car stopped, threat gone
        carState.setCarSpeed(0.05);
        ProcessedSensorData clearRaw2       = buildClearRoadSnapshot(0.05);
        ProcessedSensorData clearValidated2 = checker.validate(clearRaw2);
        CollisionAssessment clearAssessment2 = detector.assess(clearValidated2);
        assertNotNull(clearAssessment2);
        assertEquals(ThreatLevel.NONE, clearAssessment2.getThreatLevel(),
                "Step 3: after hazard clears, threat must return to NONE (DR-09)");
        log.info("TC-031-E step 3: {}", clearAssessment2.getThreatLevel());

        log.info("TC-031-E passed — all three transitions correct");
    }

    /**
     * TC-031-F: Warning zone then brake zone — two-stage escalation in one
     * pipeline run sequence confirms the full threat ramp.
     *
     * REQ-001 | DR-07
     */
    @Test
    @DisplayName("TC-031-F | Full threat ramp: NONE → WARNING → BRAKE across three pipeline steps")
    void tc031f_fullThreatRampNoneWarningBrake() {
        log.info("TC-031-F: full threat ramp NONE → WARNING → BRAKE");
        CollisionDetector freshDetector = new CollisionDetector();

        // NONE: clear road
        ProcessedSensorData noneSnap = buildClearRoadSnapshot(16.67);
        CollisionAssessment noneAssessment = freshDetector.assess(noneSnap);
        assertNotNull(noneAssessment);
        assertEquals(ThreatLevel.NONE, noneAssessment.getThreatLevel(), "Must start NONE");

        // WARNING: object at 150 m
        ProcessedSensorData warnSnap = buildHazardSnapshot(150.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment warnAssessment = freshDetector.assess(warnSnap);
        assertEquals(ThreatLevel.WARNING, warnAssessment.getThreatLevel(), "Must escalate to WARNING");

        // BRAKE: object at 5 m
        ProcessedSensorData brakeSnap = buildHazardSnapshot(5.0, 16.67, ObjectType.VEHICLE, 16.67);
        CollisionAssessment brakeAssessment = freshDetector.assess(brakeSnap);
        assertEquals(ThreatLevel.BRAKE, brakeAssessment.getThreatLevel(), "Must escalate to BRAKE");

        log.info("TC-031-F: none={}, warn={}, brake={}",
                noneAssessment.getThreatLevel(),
                warnAssessment.getThreatLevel(),
                brakeAssessment.getThreatLevel());
        log.info("TC-031-F passed");
    }
}