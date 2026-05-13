package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;

import com.team30.core.logic.RedundancyChecker;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RedundancyCheckerTest {

    private static final Logger log =
            LogManager.getLogger(RedundancyCheckerTest.class);

    private ProcessedSensorData createData(
            SensorData primary,
            SensorData redundant,
            SensorType type
    ) {

        log.debug("Creating ProcessedSensorData for type: {}", type);

        Map<SensorId, SensorData> sensorMap = new HashMap<>();

        if (primary != null) {
            log.debug("Adding PRIMARY sensor");
            sensorMap.put(SensorId.PRIMARY, primary);
        }

        if (redundant != null) {
            log.debug("Adding REDUNDANT sensor");
            sensorMap.put(SensorId.REDUNDANT, redundant);
        }

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();
        readings.put(type, sensorMap);

        return new ProcessedSensorData(
                readings,
                System.currentTimeMillis()
        );
    }

    // ---------------- RADAR TESTS ----------------

    @Test
    public void testBothRadarSensorsNull() {

        log.info("STARTING: testBothRadarSensorsNull");

        ProcessedSensorData data =
                createData(null, null, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating data with both radar sensors null");

        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testBothRadarSensorsNull");
    }

    @Test
    public void testPrimaryRadarGarbageUsesRedundant() {

        log.info("STARTING: testPrimaryRadarGarbageUsesRedundant");

        RadarData primary =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis()
                );

        RadarData redundant =
                new RadarData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        20,
                        5,
                        true
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating radar data: primary garbage, redundant valid");

        ProcessedSensorData result = checker.validate(data);

        assertNotNull(
                result.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testPrimaryRadarGarbageUsesRedundant");
    }

    @Test
    public void testRedundantRadarGarbageUsesPrimary() {

        log.info("STARTING: testRedundantRadarGarbageUsesPrimary");

        RadarData primary =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        15,
                        4,
                        true
                );

        RadarData redundant =
                new RadarData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis()
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating radar data: redundant garbage, primary valid");

        ProcessedSensorData result = checker.validate(data);

        assertNotNull(
                result.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testRedundantRadarGarbageUsesPrimary");
    }

    @Test
    public void testRadarThresholdAcceptsBoth() {

        log.info("STARTING: testRadarThresholdAcceptsBoth");

        RadarData primary =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        20,
                        5,
                        true
                );

        RadarData redundant =
                new RadarData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        22,
                        5,
                        true
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating radar data: both within threshold");

        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testRadarThresholdAcceptsBoth");
    }

    @Test
    public void testRadarThresholdRejectsRedundant() {

        log.info("STARTING: testRadarThresholdRejectsRedundant");

        RadarData primary =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        20,
                        5,
                        true
                );

        RadarData redundant =
                new RadarData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        40,
                        5,
                        true
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating radar data: redundant outside threshold");

        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testRadarThresholdRejectsRedundant");
    }

    // ---------------- CAMERA TESTS ----------------

    @Test
    public void testCameraConfidenceAcceptsBoth() {

        log.info("STARTING: testCameraConfidenceAcceptsBoth");

        CameraData primary =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.9
                );

        CameraData redundant =
                new CameraData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.8
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.CAMERA);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating camera data: both above confidence threshold");

        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.CAMERA,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testCameraConfidenceAcceptsBoth");
    }

    @Test
    public void testCameraConfidenceRejectsRedundant() {

        log.info("STARTING: testCameraConfidenceRejectsRedundant");

        CameraData primary =
                new CameraData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.9
                );

        CameraData redundant =
                new CameraData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        ObjectType.VEHICLE,
                        true,
                        0.1
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.CAMERA);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating camera data: redundant below confidence threshold");

        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.CAMERA,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testCameraConfidenceRejectsRedundant");
    }

    // ---------------- WHEEL SPEED TESTS ----------------

    @Test
    public void testWheelSpeedAcceptsBoth() {

        log.info("STARTING: testWheelSpeedAcceptsBoth");

        WheelSpeedData primary =
                new WheelSpeedData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        new double[]{10,10,10,10},
                        new double[]{10,10,10,10}
                );

        WheelSpeedData redundant =
                new WheelSpeedData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        new double[]{11,11,11,11},
                        new double[]{11,11,11,11}
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.WHEEL_SPEED);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating wheel speed data: both within acceptable range");

        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.WHEEL_SPEED,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testWheelSpeedAcceptsBoth");
    }

    @Test
    public void testWheelSpeedRejectsRedundant() {

        log.info("STARTING: testWheelSpeedRejectsRedundant");

        WheelSpeedData primary =
                new WheelSpeedData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        new double[]{10,10,10,10},
                        new double[]{10,10,10,10}
                );

        WheelSpeedData redundant =
                new WheelSpeedData(
                        SensorId.REDUNDANT,
                        System.currentTimeMillis(),
                        new double[]{30,30,30,30},
                        new double[]{30,30,30,30}
                );

        ProcessedSensorData data =
                createData(primary, redundant, SensorType.WHEEL_SPEED);

        RedundancyChecker checker = new RedundancyChecker();

        log.debug("Validating wheel speed data: redundant outside acceptable range");

        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.WHEEL_SPEED,
                        SensorId.REDUNDANT
                )
        );

        log.info("ENDING: testWheelSpeedRejectsRedundant");
    }
}
