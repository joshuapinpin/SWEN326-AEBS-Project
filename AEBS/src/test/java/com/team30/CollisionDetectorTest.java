package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CollisionDetectorTest {

    private static final Logger log =
            LogManager.getLogger(CollisionDetectorTest.class);

    @Test
    public void testNoRadarOrLidarReturnsLastAssessment() {

        log.info("STARTING: testNoRadarOrLidarReturnsLastAssessment");

        CollisionDetector detector = new CollisionDetector();

        ProcessedSensorData emptyData =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        log.debug("Calling assess() with empty sensor data");

        CollisionAssessment result = detector.assess(emptyData);

        assertNull(result);

        log.info("ENDING: testNoRadarOrLidarReturnsLastAssessment");
    }

    @Test
    public void testNoObjectDetected() {

        log.info("STARTING: testNoObjectDetected");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        10,
                        5,
                        false
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() with radar showing no object detected");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.NONE, result.getThreatLevel());

        log.info("ENDING: testNoObjectDetected");
    }

    @Test
    public void testObjectOutsideLaneReturnsNone() {

        log.info("STARTING: testObjectOutsideLaneReturnsNone");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        5,
                        10,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        false,
                        0.9
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() with camera showing object outside lane");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.NONE, result.getThreatLevel());

        log.info("ENDING: testObjectOutsideLaneReturnsNone");
    }

    @Test
    public void testNegativeRelativeSpeedReturnsNone() {

        log.info("STARTING: testNegativeRelativeSpeedReturnsNone");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        10,
                        -5,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.9
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() with negative relative speed");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.NONE, result.getThreatLevel());

        log.info("ENDING: testNegativeRelativeSpeedReturnsNone");
    }

    @Test
    public void testBrakeThreatTriggered() {

        log.info("STARTING: testBrakeThreatTriggered");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        5,
                        20,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.95
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() expecting BRAKE threat");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.BRAKE, result.getThreatLevel());

        log.info("ENDING: testBrakeThreatTriggered");
    }

    @Test
    public void testWarningThreatTriggered() {

        log.info("STARTING: testWarningThreatTriggered");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        100,
                        5,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.95
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() expecting WARNING threat");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.WARNING, result.getThreatLevel());

        log.info("ENDING: testWarningThreatTriggered");
    }

    @Test
    public void testNoThreatTriggered() {

        log.info("STARTING: testNoThreatTriggered");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        300,
                        5,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.95
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() expecting NO threat");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.NONE, result.getThreatLevel());

        log.info("ENDING: testNoThreatTriggered");
    }

    @Test
    public void testMinimumDetectionDistanceIgnored() {

        log.info("STARTING: testMinimumDetectionDistanceIgnored");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        0.1,
                        10,
                        true
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() with distance below minimum threshold");

        CollisionAssessment result = detector.assess(data);

        assertEquals(ThreatLevel.NONE, result.getThreatLevel());

        log.info("ENDING: testMinimumDetectionDistanceIgnored");
    }

    @Test
    public void testTimeToCollisionCalculated() {

        log.info("STARTING: testTimeToCollisionCalculated");

        CollisionDetector detector = new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        20,
                        10,
                        true
                );

        CameraData camera =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.9
                );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Executing assess() expecting TTC calculation");

        CollisionAssessment result = detector.assess(data);

        assertEquals(2.0, result.getTimeToCollision());

        log.info("ENDING: testTimeToCollisionCalculated");
    }
}
