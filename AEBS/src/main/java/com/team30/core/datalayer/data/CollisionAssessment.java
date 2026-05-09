package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.ObjectType;
import com.team30.core.datalayer.enums.ThreatLevel;

/**
 * Represents the result of a collision risk assessment produced by CollisionDetector.
 * Holds the threat level, time to collision, object distance, object type,
 * lane status, and sensor availability at the time of assessment.
 */
public class CollisionAssessment {
    private final ThreatLevel threatLevel; //The assessed threat level based on time to collision and object type.
    private final double timeToCollision; //Time to collision in seconds based on current speed and object distance.
    private final double distance; //Distance to the closest detected object in meters. -1.0 if no object detected.
    private final ObjectType objectType; //The classified type of the detected object.
    private final boolean objectInLane; //True if the detected object is in the current lane of the vehicle.
    private final boolean radarAvailable; //True if radar data was available during this assessment.
    private final boolean lidarAvailable; //True if lidar data was available during this assessment.
    private final boolean cameraAvailable; //True if camera data was available during this assessment.

    /**
     * Constructs a CollisionAssessment with all fields set.
     * @param threatLevel     the assessed threat level
     * @param timeToCollision time to collision in seconds, -1.0 if not applicable
     * @param distance        distance to object in meters, -1.0 if not applicable
     * @param objectType      the classified object type, null if unavailable
     * @param objectInLane    true if the object is in the current lane
     * @param radarAvailable  true if radar data was available
     * @param lidarAvailable  true if lidar data was available
     * @param cameraAvailable true if camera data was available
     */
    public CollisionAssessment(ThreatLevel threatLevel, double timeToCollision, double distance,
            ObjectType objectType, boolean objectInLane, boolean radarAvailable, boolean lidarAvailable,
            boolean cameraAvailable) {
        this.threatLevel = threatLevel;
        this.timeToCollision = timeToCollision;
        this.distance = distance;
        this.objectType = objectType;
        this.objectInLane = objectInLane;
        this.radarAvailable = radarAvailable;
        this.lidarAvailable = lidarAvailable;
        this.cameraAvailable = cameraAvailable;
    }

    /**
     * Returns the assessed threat level.
     * @return the ThreatLevel of this assessment
     */
    public ThreatLevel getThreatLevel() { return threatLevel; }

    /**
     * Returns the time to collision in seconds.
     * Returns -1.0 if no object detected or TTC cannot be calculated.
     * @return time to collision in seconds
     */
    public double getTimeToCollision() { return timeToCollision; }

    /**
     * Returns the distance to the closest detected object in meters.
     * Returns -1.0 if no object detected.
     * @return distance in meters
     */
    public double getDistance() { return distance; }

    /**
     * Returns the classified type of the detected object.
     * Returns null if no object detected or camera unavailable.
     * @return the ObjectType of the detected object
     */
    public ObjectType getObjectType() { return objectType; }

    /**
     * Returns true if the detected object is in the current lane.
     * If false, CollisionDetector returns NONE regardless of TTC.
     * @return true if object is in current lane
     */
    public boolean isObjectInLane() { return objectInLane; }

    /**
     * Returns true if radar data was available during this assessment.
     * Used by FaultHandler to determine sensor availability.
     * @return true if radar was available
     */
    public boolean isRadarAvailable() { return radarAvailable; }

    /**
     * Returns true if lidar data was available during this assessment.
     * Used by FaultHandler to determine sensor availability.
     * @return true if lidar was available
     */
    public boolean isLidarAvailable() { return lidarAvailable; }

    /**
     * Returns true if camera data was available during this assessment.
     * Used by FaultHandler to determine sensor availability.
     * @return true if camera was available
     */
    public boolean isCameraAvailable() { return cameraAvailable; }

    /**
     * Returns a string representation of this assessment for debugging.
     * @return string representation of all assessment fields
     */
    @Override
    public String toString() {
        return "CollisionAssessment{" +
                "threatLevel=" + threatLevel +
                ", timeToCollision=" + timeToCollision +
                ", distance=" + distance +
                ", objectType=" + objectType +
                ", objectInLane=" + objectInLane +
                ", radarAvailable=" + radarAvailable +
                ", lidarAvailable=" + lidarAvailable +
                ", cameraAvailable=" + cameraAvailable +
                '}';
    }
}