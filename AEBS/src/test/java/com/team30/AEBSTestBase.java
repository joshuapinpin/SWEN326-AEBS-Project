package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.simulation.state.CarState;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class shared by all AEBS JUnit test suites.
 *
 * Provides convenience factory methods for building ProcessedSensorData
 * snapshots and common CarState configurations so individual test files
 * stay focused on assertions rather than boilerplate construction.
 *
 * Log4j2 is available via the protected {@code logger} field so every
 * subclass can emit structured log output without declaring its own logger.
 */
public abstract class AEBSTestBase {

    protected static final Logger logger = LogManager.getLogger(AEBSTestBase.class);

    // ---------------------------------------------------------------
    // Standard physics constants that mirror the production code
    // ---------------------------------------------------------------
    /** Deceleration rate used by BrakeSystemController (m/s²). */
    protected static final double BRAKE_DECELERATION = 8.0;

    /** Safety buffer added to the physics stopping distance (m). */
    protected static final double SAFETY_BUFFER = 5.0;

    /** Minimum detectable object distance (m). */
    protected static final double MIN_DETECTION_DISTANCE = 0.5;

    /** Outer edge of the warning zone (m). */
    protected static final double WARNING_DISTANCE = 200.0;

    // ---------------------------------------------------------------
    // CarState factory
    // ---------------------------------------------------------------

    /**
     * Creates a standard CarState for a vehicle cruising at the given speed.
     *
     * @param speedMs speed in m/s
     * @return a fully-initialised CarState in CRUISING mode
     */
    protected CarState makeCruisingCarState(double speedMs) {
        double[] rpm = {500, 500, 500, 500};
        return new CarState(
                speedMs, speedMs, rpm,
                DrivingMode.CRUISING, 0.0, 2.0,
                WeatherCondition.CLEAR, LightCondition.DAY
        );
    }

    /**
     * Creates a CarState already in FAIL_SAFE mode (speed = 0).
     */
    protected CarState makeFailSafeCarState() {
        double[] rpm = {0, 0, 0, 0};
        return new CarState(
                0.0, 0.0, rpm,
                DrivingMode.FAIL_SAFE, 8.0, 0.0,
                WeatherCondition.CLEAR, LightCondition.DAY
        );
    }

    // ---------------------------------------------------------------
    // ProcessedSensorData builders
    // ---------------------------------------------------------------

    /**
     * Builds a ProcessedSensorData snapshot from an arbitrary map of readings.
     * Convenience wrapper so tests don't repeat the constructor call.
     */
    protected ProcessedSensorData buildSnapshot(
            Map<SensorType, Map<SensorId, SensorData>> readings) {
        return new ProcessedSensorData(readings, System.currentTimeMillis());
    }

    /**
     * Returns a snapshot with healthy radar + lidar + camera + wheel-speed data,
     * representing a clear road with no object detected.
     *
     * @param carSpeedMs the vehicle's current speed in m/s (used to derive RPM)
     */
    protected ProcessedSensorData buildClearRoadSnapshot(double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        // Radar – nothing detected
        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, -1.0, 0.0, false));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, -1.0, 0.0, false));
        readings.put(SensorType.RADAR, radarMap);

        // Lidar – nothing detected
        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, -1.0, 0.0, false));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, -1.0, 0.0, false));
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera – nothing in lane
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, ObjectType.UNKNOWN, false, 0.9));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, ObjectType.UNKNOWN, false, 0.9));
        readings.put(SensorType.CAMERA, cameraMap);

        // Wheel speed
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

    /**
     * Builds a snapshot where radar (and matching lidar) detects an object at the
     * given distance and relative speed, camera classifies it as the given type
     * and marks it as in-lane.
     *
     * @param distanceM    object distance in metres
     * @param relSpeedMs   relative closing speed in m/s (positive = approaching)
     * @param objectType   camera classification
     * @param carSpeedMs   vehicle speed in m/s
     */
    protected ProcessedSensorData buildHazardSnapshot(
            double distanceM, double relSpeedMs,
            ObjectType objectType, double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,
                new RadarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
        radarMap.put(SensorId.REDUNDANT,
                new RadarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        lidarMap.put(SensorId.PRIMARY,
                new LidarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
        lidarMap.put(SensorId.REDUNDANT,
                new LidarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        readings.put(SensorType.LIDAR, lidarMap);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        cameraMap.put(SensorId.PRIMARY,
                new CameraData(SensorId.PRIMARY, ts, objectType, true, 0.95));
        cameraMap.put(SensorId.REDUNDANT,
                new CameraData(SensorId.REDUNDANT, ts, objectType, true, 0.93));
        readings.put(SensorType.CAMERA, cameraMap);

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

    /**
     * Builds a snapshot where both primary and redundant of the given sensor type
     * are garbage (failed), while every other sensor type remains healthy.
     *
     * @param failedType   the SensorType that has completely failed
     * @param distanceM    distance used for the still-healthy range sensors
     * @param relSpeedMs   relative speed for the still-healthy range sensors
     * @param carSpeedMs   vehicle speed in m/s
     */
    protected ProcessedSensorData buildBothFailedSnapshot(
            SensorType failedType, double distanceM,
            double relSpeedMs, double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        // Radar
        Map<SensorId, SensorData> radarMap = new HashMap<>();
        if (failedType == SensorType.RADAR) {
            radarMap.put(SensorId.PRIMARY,   new RadarData(SensorId.PRIMARY, ts));
            radarMap.put(SensorId.REDUNDANT, new RadarData(SensorId.REDUNDANT, ts));
        } else {
            radarMap.put(SensorId.PRIMARY,
                    new RadarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
            radarMap.put(SensorId.REDUNDANT,
                    new RadarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        }
        readings.put(SensorType.RADAR, radarMap);

        // Lidar
        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        if (failedType == SensorType.LIDAR) {
            lidarMap.put(SensorId.PRIMARY,   new LidarData(SensorId.PRIMARY, ts));
            lidarMap.put(SensorId.REDUNDANT, new LidarData(SensorId.REDUNDANT, ts));
        } else {
            lidarMap.put(SensorId.PRIMARY,
                    new LidarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
            lidarMap.put(SensorId.REDUNDANT,
                    new LidarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        }
        readings.put(SensorType.LIDAR, lidarMap);

        // Camera
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        if (failedType == SensorType.CAMERA) {
            cameraMap.put(SensorId.PRIMARY,   new CameraData(SensorId.PRIMARY, ts));
            cameraMap.put(SensorId.REDUNDANT, new CameraData(SensorId.REDUNDANT, ts));
        } else {
            cameraMap.put(SensorId.PRIMARY,
                    new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
            cameraMap.put(SensorId.REDUNDANT,
                    new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        }
        readings.put(SensorType.CAMERA, cameraMap);

        // Wheel speed
        double[] rpm = {500, 500, 500, 500};
        double[] spd = {carSpeedMs, carSpeedMs, carSpeedMs, carSpeedMs};
        Map<SensorId, SensorData> wheelMap = new HashMap<>();
        if (failedType == SensorType.WHEEL_SPEED) {
            wheelMap.put(SensorId.PRIMARY,   new WheelSpeedData(SensorId.PRIMARY, ts));
            wheelMap.put(SensorId.REDUNDANT, new WheelSpeedData(SensorId.REDUNDANT, ts));
        } else {
            wheelMap.put(SensorId.PRIMARY,
                    new WheelSpeedData(SensorId.PRIMARY, ts, rpm, spd));
            wheelMap.put(SensorId.REDUNDANT,
                    new WheelSpeedData(SensorId.REDUNDANT, ts, rpm, spd));
        }
        readings.put(SensorType.WHEEL_SPEED, wheelMap);

        return buildSnapshot(readings);
    }

    /**
     * Builds a snapshot where only the primary sensor of the given type is garbage;
     * the redundant sensor is healthy and should be promoted by RedundancyChecker.
     *
     * @param failedType  the SensorType whose primary has failed
     * @param distanceM   distance used for healthy range sensors
     * @param relSpeedMs  relative speed for healthy range sensors
     * @param carSpeedMs  vehicle speed in m/s
     */
    protected ProcessedSensorData buildPrimaryFailedSnapshot(
            SensorType failedType, double distanceM,
            double relSpeedMs, double carSpeedMs) {
        long ts = System.currentTimeMillis();
        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        if (failedType == SensorType.RADAR) {
            radarMap.put(SensorId.PRIMARY,   new RadarData(SensorId.PRIMARY, ts));
            radarMap.put(SensorId.REDUNDANT,
                    new RadarData(SensorId.REDUNDANT, ts, distanceM, relSpeedMs, true));
        } else {
            radarMap.put(SensorId.PRIMARY,
                    new RadarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
            radarMap.put(SensorId.REDUNDANT,
                    new RadarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        }
        readings.put(SensorType.RADAR, radarMap);

        Map<SensorId, SensorData> lidarMap = new HashMap<>();
        if (failedType == SensorType.LIDAR) {
            lidarMap.put(SensorId.PRIMARY,   new LidarData(SensorId.PRIMARY, ts));
            lidarMap.put(SensorId.REDUNDANT,
                    new LidarData(SensorId.REDUNDANT, ts, distanceM, relSpeedMs, true));
        } else {
            lidarMap.put(SensorId.PRIMARY,
                    new LidarData(SensorId.PRIMARY, ts, distanceM, relSpeedMs, true));
            lidarMap.put(SensorId.REDUNDANT,
                    new LidarData(SensorId.REDUNDANT, ts, distanceM + 0.5, relSpeedMs, true));
        }
        readings.put(SensorType.LIDAR, lidarMap);

        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        if (failedType == SensorType.CAMERA) {
            cameraMap.put(SensorId.PRIMARY,   new CameraData(SensorId.PRIMARY, ts));
            cameraMap.put(SensorId.REDUNDANT,
                    new CameraData(SensorId.REDUNDANT, ts, ObjectType.PEDESTRIAN, true, 0.88));
        } else {
            cameraMap.put(SensorId.PRIMARY,
                    new CameraData(SensorId.PRIMARY, ts, ObjectType.VEHICLE, true, 0.95));
            cameraMap.put(SensorId.REDUNDANT,
                    new CameraData(SensorId.REDUNDANT, ts, ObjectType.VEHICLE, true, 0.93));
        }
        readings.put(SensorType.CAMERA, cameraMap);

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

    /**
     * Computes the physics-based required braking distance given a closing
     * speed, the same way CollisionDetector does.
     *
     * @param relSpeedMs closing speed in m/s
     * @return minimum distance at which braking must be initiated (m)
     */
    protected double requiredBrakeDistance(double relSpeedMs) {
        double stoppingDist = (relSpeedMs * relSpeedMs) / (2.0 * BRAKE_DECELERATION);
        return stoppingDist + SAFETY_BUFFER;
    }
}