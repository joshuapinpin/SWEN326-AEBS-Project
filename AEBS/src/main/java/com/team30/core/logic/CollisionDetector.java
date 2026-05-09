package com.team30.core.logic;

import com.team30.core.datalayer.data.CameraData;
import com.team30.core.datalayer.data.CollisionAssessment;
import com.team30.core.datalayer.data.LidarData;
import com.team30.core.datalayer.data.RadarData;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.ObjectType;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.ThreatLevel;

import java.util.EnumMap;
import java.util.Map;

public class CollisionDetector {

    private CollisionAssessment lastAssessment;

    // TTC seconds at which a WARNING is triggered per object type
    private final Map<ObjectType, Double> warnThresholds;

    // TTC seconds at which BRAKE is triggered per object type
    private final Map<ObjectType, Double> brakeThresholds;

    public CollisionDetector() {
        warnThresholds = new EnumMap<>(ObjectType.class);
        brakeThresholds = new EnumMap<>(ObjectType.class);

        // Pedestrians get more time — they're unpredictable and vulnerable
        warnThresholds.put(ObjectType.VEHICLE,     3.5);
        warnThresholds.put(ObjectType.PEDESTRIAN,  5.0);
        warnThresholds.put(ObjectType.UNKNOWN,     4.0);

        brakeThresholds.put(ObjectType.VEHICLE,    1.5);
        brakeThresholds.put(ObjectType.PEDESTRIAN, 2.5);
        brakeThresholds.put(ObjectType.UNKNOWN,    2.0);
    }

    /**
     * Assesses collision risk from the latest sensor snapshot.
     * Skips assessment if no new radar or lidar data is present.
     * Returns NONE if no object is detected or object is not in lane.
     * Otherwise calculates TTC and determines threat level from object type thresholds.
     *
     * @param data the latest ProcessedSensorData snapshot
     * @return CollisionAssessment result, or lastAssessment if skipped
     */
    public CollisionAssessment assess(ProcessedSensorData data) {
        // Skip if no new radar or lidar — nothing useful to assess
        if (!data.hasNewRadarOrLidar()) {
            return lastAssessment;
        }

        boolean radarAvailable  = data.isSensorAvailable(SensorType.RADAR,  SensorId.PRIMARY)  || data.isSensorAvailable(SensorType.RADAR,  SensorId.REDUNDANT);
        boolean lidarAvailable  = data.isSensorAvailable(SensorType.LIDAR,  SensorId.PRIMARY)  || data.isSensorAvailable(SensorType.LIDAR,  SensorId.REDUNDANT);
        boolean cameraAvailable = data.isSensorAvailable(SensorType.CAMERA, SensorId.PRIMARY)  || data.isSensorAvailable(SensorType.CAMERA, SensorId.REDUNDANT);

        // Get best distance reading — prefer radar, fall back to lidar
        double distance      = -1.0;
        double relativeSpeed = 0.0;
        boolean objectDetected = false;

        RadarData radar = getBestReading(data, SensorType.RADAR, RadarData.class);
        LidarData lidar = getBestReading(data, SensorType.LIDAR, LidarData.class);

        if (radar != null && radar.isObjectDetected()) {
            distance = radar.getDistance();
            relativeSpeed  = radar.getRelativeSpeed();
            objectDetected = true;
        }
        else if (lidar != null && lidar.isObjectDetected()) {
            distance = lidar.getDistance();
            relativeSpeed  = lidar.getRelativeSpeed();
            objectDetected = true;
        }

        // No object detected — return NONE
        if (!objectDetected) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, -1.0, null, false,
                    radarAvailable, lidarAvailable, cameraAvailable);
            return lastAssessment;
        }

        // Get object type and lane status from camera
        ObjectType objectType = ObjectType.UNKNOWN;
        boolean objectInLane  = false;

        CameraData camera = getBestReading(data, SensorType.CAMERA, CameraData.class);
        if (camera != null) {
            objectType   = camera.getClassification();
            objectInLane = camera.isInCurrentLane();
        }

        // Object not in lane — no threat regardless of TTC
        if (!objectInLane) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, distance, objectType, false,
                    radarAvailable, lidarAvailable, cameraAvailable);
            return lastAssessment;
        }

        // Calculate TTC — if object is moving away, no threat
        if (relativeSpeed <= 0) {
            lastAssessment = new CollisionAssessment(
                    ThreatLevel.NONE, -1.0, distance, objectType, true,
                    radarAvailable, lidarAvailable, cameraAvailable);
            return lastAssessment;
        }

        double ttc = distance / relativeSpeed;

        // Determine threat level from per-type thresholds
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
                radarAvailable, lidarAvailable, cameraAvailable);
        return lastAssessment;
    }

    /**
     * Returns the primary reading of the given type if available and not garbage,
     * otherwise falls back to redundant. Returns null if neither is usable.
     */
    @SuppressWarnings("unchecked")
    private <T extends SensorData> T getBestReading(ProcessedSensorData data,
                                                    SensorType type, Class<T> clazz) {
        SensorData primary   = data.getSensorData(type, SensorId.PRIMARY);
        SensorData redundant = data.getSensorData(type, SensorId.REDUNDANT);

        if (primary != null && !primary.isGarbage()   && clazz.isInstance(primary))   return (T) primary;
        if (redundant != null && !redundant.isGarbage() && clazz.isInstance(redundant)) return (T) redundant;
        return null;
    }

    public CollisionAssessment getLastAssessment() { return lastAssessment; }
}