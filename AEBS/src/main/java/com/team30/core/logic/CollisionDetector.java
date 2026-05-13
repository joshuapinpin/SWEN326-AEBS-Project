package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * CollisionDetector is responsible for analyzing processed sensor data to assess potential collision threats.
 * It evaluates the distance, relative speed, and object classification to determine if braking or warning is necessary.
 */
public class CollisionDetector {
    private static final double MIN_DETECTION_DISTANCE = 0.5;
    private static final double WARNING_DISTANCE = 200.0; // Object visible + warning starts here

    private final Map<ObjectType, Double> brakeThresholds;
    private CollisionAssessment lastAssessment;

    /**
     * Initializes the CollisionDetector with predefined braking thresholds for different object types.
     * These thresholds represent the minimum Time-to-Collision (TTC) required to trigger braking for each object type.
     */
    public CollisionDetector() {
        brakeThresholds = new EnumMap<>(ObjectType.class);

        brakeThresholds.put(ObjectType.VEHICLE, 1.5);
        brakeThresholds.put(ObjectType.PEDESTRIAN, 2.5);
        brakeThresholds.put(ObjectType.UNKNOWN, 2.0);
    }

    /**
     * Assesses the collision threat level based on the latest processed sensor data.
     * It considers the availability of radar, lidar, and camera data, and calculates the Time-to-Collision (TTC)
     * to determine if braking or warning is necessary.
     * @param data The processed sensor data containing the latest readings from radar, lidar, and camera sensors.
     * @return A CollisionAssessment object containing the threat level, TTC, distance, object type, and sensor availability information.
     */
    public CollisionAssessment assess(ProcessedSensorData data) {
        assert data != null : "ProcessedSensorData must not be null";
        assert data.getTimestamp() > 0 : "ProcessedSensorData must have valid timestamp";

        if (!data.hasNewRadarOrLidar()) {
            return lastAssessment;
        }

        boolean radarAvailable =
                data.isSensorAvailable(SensorType.RADAR, SensorId.PRIMARY)
                        || data.isSensorAvailable(SensorType.RADAR, SensorId.REDUNDANT);

        boolean lidarAvailable =
                data.isSensorAvailable(SensorType.LIDAR, SensorId.PRIMARY)
                        || data.isSensorAvailable(SensorType.LIDAR, SensorId.REDUNDANT);

        boolean cameraAvailable =
                data.isSensorAvailable(SensorType.CAMERA, SensorId.PRIMARY)
                        || data.isSensorAvailable(SensorType.CAMERA, SensorId.REDUNDANT);

        double distance = -1.0;
        double relativeSpeed = 0.0;
        boolean objectDetected = false;

        RadarData radar =
                getBestReading(data, SensorType.RADAR, RadarData.class);

        LidarData lidar =
                getBestReading(data, SensorType.LIDAR, LidarData.class);

        if (radar != null
                && radar.isObjectDetected()
                && radar.getDistance() >= MIN_DETECTION_DISTANCE) {

            distance = radar.getDistance();
            relativeSpeed = radar.getRelativeSpeed();
            objectDetected = true;

        } else if (lidar != null
                && lidar.isObjectDetected()
                && lidar.getDistance() >= MIN_DETECTION_DISTANCE) {

            distance = lidar.getDistance();
            relativeSpeed = lidar.getRelativeSpeed();
            objectDetected = true;
        }

        if (!objectDetected) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE,
                    -1.0,
                    -1.0,
                    null,
                    false,
                    radarAvailable,
                    lidarAvailable,
                    cameraAvailable
            );

            return lastAssessment;
        }

        ObjectType objectType = ObjectType.UNKNOWN;
        boolean objectInLane = true;

        CameraData camera = getBestReading(data, SensorType.CAMERA, CameraData.class);

        if (camera != null) {
            objectType = camera.getClassification();
            objectInLane = camera.isInCurrentLane();
        }

        if (!objectInLane) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE,
                    -1.0,
                    distance,
                    objectType,
                    false,
                    radarAvailable,
                    lidarAvailable,
                    cameraAvailable
            );

            return lastAssessment;
        }

        if (relativeSpeed <= 0) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE,
                    -1.0,
                    distance,
                    objectType,
                    true,
                    radarAvailable,
                    lidarAvailable,
                    cameraAvailable
            );

            return lastAssessment;
        }

        double ttc = distance / relativeSpeed;

        double deceleration = 8.0;

        // Driver/system reaction time
        double reactionTime = 0.5;

        // Distance travelled before braking starts
        double reactionDistance =
                relativeSpeed * reactionTime;

        // Physics braking distance
        double stoppingDistance =
                relativeSpeed * relativeSpeed
                        / (2.0 * deceleration);

        // Extra safety margin
        double safetyBuffer = 10.0;

        double requiredBrakeDistance = reactionDistance + stoppingDistance + safetyBuffer;

        ThreatLevel threat;

        // Brake dynamically based on speed
        if (distance <= requiredBrakeDistance) {

            threat = ThreatLevel.BRAKE;

        }
        // Warning zone
        else if (distance <= WARNING_DISTANCE) {

            threat = ThreatLevel.WARNING;

        }
        // Too far away
        else {

            threat = ThreatLevel.NONE;
        }
        lastAssessment = new CollisionAssessment(
                threat,
                ttc,
                distance,
                objectType,
                true,
                radarAvailable,
                lidarAvailable,
                cameraAvailable
        );

        return lastAssessment;
    }

    @SuppressWarnings("unchecked")
    private <T extends SensorData> T getBestReading(
            ProcessedSensorData data,
            SensorType type,
            Class<T> clazz
    ) {
        assert data != null : "ProcessedSensorData must not be null";
        assert type != null : "SensorType must not be null";
        assert clazz != null : "Class type must not be null";

        SensorData primary =
                data.getSensorData(type, SensorId.PRIMARY);

        SensorData redundant =
                data.getSensorData(type, SensorId.REDUNDANT);

        if (primary != null
                && !primary.isGarbage()
                && clazz.isInstance(primary)) {

            return (T) primary;
        }

        if (redundant != null
                && !redundant.isGarbage()
                && clazz.isInstance(redundant)) {

            return (T) redundant;
        }

        return null;
    }

    public CollisionAssessment getLastAssessment() {
        return lastAssessment;
    }
}