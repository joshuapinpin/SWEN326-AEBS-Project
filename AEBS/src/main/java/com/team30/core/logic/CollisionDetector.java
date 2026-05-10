package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;

import java.util.EnumMap;
import java.util.Map;

public class CollisionDetector {

    private CollisionAssessment lastAssessment;

    private static final double MIN_DETECTION_DISTANCE = 0.5;
    private static final double MAX_DETECTION_DISTANCE = 200.0;

    private final Map<ObjectType, Double> warnThresholds;
    private final Map<ObjectType, Double> brakeThresholds;

    public CollisionDetector() {
        warnThresholds = new EnumMap<>(ObjectType.class);
        brakeThresholds = new EnumMap<>(ObjectType.class);

        warnThresholds.put(ObjectType.VEHICLE, 3.5);
        warnThresholds.put(ObjectType.PEDESTRIAN, 5.0);
        warnThresholds.put(ObjectType.UNKNOWN, 4.0);

        brakeThresholds.put(ObjectType.VEHICLE, 1.5);
        brakeThresholds.put(ObjectType.PEDESTRIAN, 2.5);
        brakeThresholds.put(ObjectType.UNKNOWN, 2.0);
    }

    public CollisionAssessment assess(ProcessedSensorData data) {
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

        RadarData radar = getBestReading(data, SensorType.RADAR, RadarData.class);
        LidarData lidar = getBestReading(data, SensorType.LIDAR, LidarData.class);

        if (radar != null
                && radar.isObjectDetected()
                && radar.getDistance() >= MIN_DETECTION_DISTANCE
                && radar.getDistance() <= MAX_DETECTION_DISTANCE) {

            distance = radar.getDistance();
            relativeSpeed = radar.getRelativeSpeed();
            objectDetected = true;

        } else if (lidar != null
                && lidar.isObjectDetected()
                && lidar.getDistance() >= MIN_DETECTION_DISTANCE
                && lidar.getDistance() <= MAX_DETECTION_DISTANCE) {

            distance = lidar.getDistance();
            relativeSpeed = lidar.getRelativeSpeed();
            objectDetected = true;
        }

        if (!objectDetected) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, -1.0, null, false,
                    radarAvailable, lidarAvailable, cameraAvailable
            );
            return lastAssessment;
        }

        ObjectType objectType = ObjectType.UNKNOWN;
        boolean objectInLane = false;

        CameraData camera = getBestReading(data, SensorType.CAMERA, CameraData.class);

        if (camera != null) {
            objectType = camera.getClassification();
            objectInLane = camera.isInCurrentLane();
        }

        if (!objectInLane) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, distance, objectType, false,
                    radarAvailable, lidarAvailable, cameraAvailable
            );
            return lastAssessment;
        }

        if (relativeSpeed <= 0) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, distance, objectType, true,
                    radarAvailable, lidarAvailable, cameraAvailable
            );
            return lastAssessment;
        }

        double ttc = distance / relativeSpeed;

        ThreatLevel threat;

        if (ttc <= brakeThresholds.get(objectType)) {
            threat = ThreatLevel.BRAKE;
        } else if (ttc <= warnThresholds.get(objectType)) {
            threat = ThreatLevel.WARNING;
        } else {
            threat = ThreatLevel.NONE;
        }

        lastAssessment = new CollisionAssessment(
                threat, ttc, distance, objectType, true,
                radarAvailable, lidarAvailable, cameraAvailable
        );

        return lastAssessment;
    }

    @SuppressWarnings("unchecked")
    private <T extends SensorData> T getBestReading(
            ProcessedSensorData data,
            SensorType type,
            Class<T> clazz
    ) {
        SensorData primary = data.getSensorData(type, SensorId.PRIMARY);
        SensorData redundant = data.getSensorData(type, SensorId.REDUNDANT);

        if (primary != null && !primary.isGarbage() && clazz.isInstance(primary)) {
            return (T) primary;
        }

        if (redundant != null && !redundant.isGarbage() && clazz.isInstance(redundant)) {
            return (T) redundant;
        }

        return null;
    }

    public CollisionAssessment getLastAssessment() {
        return lastAssessment;
    }
}