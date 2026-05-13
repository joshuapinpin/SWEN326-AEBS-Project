package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessedSensorDataTest {

    private static final Logger log =
            LogManager.getLogger(ProcessedSensorDataTest.class);

    @Test
    public void testSensorAvailableReturnsTrue() {

        log.info("STARTING: testSensorAvailableReturnsTrue");

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

        log.debug("Checking sensor availability");

        assertTrue(
                data.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testSensorAvailableReturnsTrue");
    }

    @Test
    public void testSensorAvailableReturnsFalse() {

        log.info("STARTING: testSensorAvailableReturnsFalse");

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        log.debug("Checking unavailable sensor");

        assertFalse(
                data.isSensorAvailable(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testSensorAvailableReturnsFalse");
    }

    @Test
    public void testGetSensorDataReturnsNull() {

        log.info("STARTING: testGetSensorDataReturnsNull");

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        log.debug("Checking null sensor data path");

        assertNull(
                data.getSensorData(
                        SensorType.RADAR,
                        SensorId.PRIMARY
                )
        );

        log.info("ENDING: testGetSensorDataReturnsNull");
    }

    @Test
    public void testHasNewRadarOrLidarReturnsTrue() {

        log.info("STARTING: testHasNewRadarOrLidarReturnsTrue");

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

        log.debug("Checking radar/lidar detection branch");

        assertTrue(data.hasNewRadarOrLidar());

        log.info("ENDING: testHasNewRadarOrLidarReturnsTrue");
    }

    @Test
    public void testHasNewRadarOrLidarReturnsFalse() {

        log.info("STARTING: testHasNewRadarOrLidarReturnsFalse");

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        System.currentTimeMillis()
                );

        log.debug("Checking empty readings branch");

        assertFalse(data.hasNewRadarOrLidar());

        log.info("ENDING: testHasNewRadarOrLidarReturnsFalse");
    }

    @Test
    public void testTimestampStoredCorrectly() {

        log.info("STARTING: testTimestampStoredCorrectly");

        long time = System.currentTimeMillis();

        ProcessedSensorData data =
                new ProcessedSensorData(
                        new HashMap<>(),
                        time
                );

        log.debug("Checking timestamp getter");

        assertEquals(time, data.getTimestamp());

        log.info("ENDING: testTimestampStoredCorrectly");
    }

    @Test
    public void testReadingsReturnedCorrectly() {

        log.info("STARTING: testReadingsReturnedCorrectly");

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        log.debug("Checking readings getter");

        assertEquals(readings, data.getReadings());

        log.info("ENDING: testReadingsReturnedCorrectly");
    }
}