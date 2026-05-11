package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessedSensorDataTest {

    @Test
    public void testSensorAvailableReturnsTrue() {

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        10,
                        5,
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

        assertTrue(
                data.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testSensorAvailableReturnsFalse() {

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        assertFalse(
                data.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testGetSensorDataReturnsNull() {

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        assertNull(
                data.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );
    }

    @Test
    public void testHasNewRadarOrLidarReturnsTrue() {

        RadarData radar =
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        5,
                        2,
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

        assertTrue(data.hasNewRadarOrLidar());
    }

    @Test
    public void testHasNewRadarOrLidarReturnsFalse() {

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        assertFalse(data.hasNewRadarOrLidar());
    }

    @Test
    public void testTimestampStoredCorrectly() {

        long time = System.currentTimeMillis();

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        time
                );

        assertEquals(time, data.getTimestamp());
    }

    @Test
    public void testReadingsReturnedCorrectly() {

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        assertEquals(readings, data.getReadings());
    }
}