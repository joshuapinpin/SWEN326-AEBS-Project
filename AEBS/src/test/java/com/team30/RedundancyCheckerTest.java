package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RedundancyCheckerTest {

    private ProcessedSensorData createData(
            SensorData primary,
            SensorData redundant,
            SensorType type
    ) {

        Map<SensorId, SensorData> sensorMap = new HashMap<>();

        if (primary != null) {
            sensorMap.put(SensorId.PRIMARY, primary);
        }

        if (redundant != null) {
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

        ProcessedSensorData data =
                createData(null, null, SensorType.RADAR);

        RedundancyChecker checker = new RedundancyChecker();
        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testPrimaryRadarGarbageUsesRedundant() {

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
        ProcessedSensorData result = checker.validate(data);

        assertNotNull(
                result.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testRedundantRadarGarbageUsesPrimary() {

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
        ProcessedSensorData result = checker.validate(data);

        assertNotNull(
                result.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testRadarThresholdAcceptsBoth() {

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
        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.REDUNDANT
                )
        );
    }

    @Test
    public void testRadarThresholdRejectsRedundant() {

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
        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.REDUNDANT
                )
        );
    }

    // ---------------- CAMERA TESTS ----------------

    @Test
    public void testCameraConfidenceAcceptsBoth() {

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
        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.CAMERA,
                        SensorId.REDUNDANT
                )
        );
    }

    @Test
    public void testCameraConfidenceRejectsRedundant() {

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
        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.CAMERA,
                        SensorId.REDUNDANT
                )
        );
    }

    // ---------------- WHEEL SPEED TESTS ----------------

    @Test
    public void testWheelSpeedAcceptsBoth() {

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
        ProcessedSensorData result = checker.validate(data);

        assertTrue(
                result.isSensorAvailable(
                        SensorType.WHEEL_SPEED,
                        SensorId.REDUNDANT
                )
        );
    }

    @Test
    public void testWheelSpeedRejectsRedundant() {

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
        ProcessedSensorData result = checker.validate(data);

        assertFalse(
                result.isSensorAvailable(
                        SensorType.WHEEL_SPEED,
                        SensorId.REDUNDANT
                )
        );
    }
}
