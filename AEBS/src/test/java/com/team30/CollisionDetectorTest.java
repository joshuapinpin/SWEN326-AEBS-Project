package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CollisionDetectorTest {

    @Test
    public void testNoRadarOrLidarReturnsLastAssessment() {

        CollisionDetector detector =
                new CollisionDetector();

        ProcessedSensorData emptyData =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        CollisionAssessment result =
                detector.assess(emptyData);

        assertNull(result);
    }

    @Test
    public void testNoObjectDetected() {

        CollisionDetector detector =
                new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        10,
                        5,
                        false
                );

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        readings.put(SensorType.RADAR, radarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.NONE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testObjectOutsideLaneReturnsNone() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.NONE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testNegativeRelativeSpeedReturnsNone() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.NONE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testBrakeThreatTriggered() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.BRAKE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testWarningThreatTriggered() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.WARNING,
                result.getThreatLevel()
        );
    }

    @Test
    public void testNoThreatTriggered() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.NONE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testMinimumDetectionDistanceIgnored() {

        CollisionDetector detector =
                new CollisionDetector();

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        0.1,
                        10,
                        true
                );

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        readings.put(SensorType.RADAR, radarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                ThreatLevel.NONE,
                result.getThreatLevel()
        );
    }

    @Test
    public void testTimeToCollisionCalculated() {

        CollisionDetector detector =
                new CollisionDetector();

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

        Map<SensorId, SensorData> radarMap =
                new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorId, SensorData> cameraMap =
                new HashMap<>();

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

        CollisionAssessment result =
                detector.assess(data);

        assertEquals(
                2.0,
                result.getTimeToCollision()
        );
    }
}