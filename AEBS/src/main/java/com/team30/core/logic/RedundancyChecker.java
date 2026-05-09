package com.team30.core.logic;

import com.team30.core.datalayer.data.CameraData;
import com.team30.core.datalayer.data.LidarData;
import com.team30.core.datalayer.data.ProcessedSensorData;
import com.team30.core.datalayer.data.RadarData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.data.WheelSpeedData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates sensor data by comparing primary and redundant sensor readings
 * for each sensor type following the 1oo2 redundant architecture.
 * Uses the isGarbage flag to detect failed sensors and compares primary
 * vs redundant values against configurable thresholds to detect disagreement.
 * If primary is garbage, promotes the redundant sensor. If both are garbage,
 * the sensor type is marked fully unavailable in the returned ProcessedSensorData.
 */
public class RedundancyChecker {
    private static final double RADAR_DISTANCE_THRESHOLD = 5.0;
    private static final double LIDAR_DISTANCE_THRESHOLD = 5.0;
    private static final double CAMERA_CONFIDENCE_THRESHOLD = 0.2;
    private static final double WHEEL_SPEED_THRESHOLD = 2.0;

    /**
     * Validates the ProcessedSensorData by checking each sensor type
     * for garbage readings and value disagreement between primary and redundant.
     * Promotes redundant if primary is garbage.
     * Marks sensor type unavailable if both are garbage.
     * @param data the ProcessedSensorData to validate
     * @return cleaned ProcessedSensorData with only trustworthy readings
     */
    public ProcessedSensorData validate(ProcessedSensorData data) {
        Map<SensorType, Map<SensorId, SensorData>> cleanedReadings = new HashMap<>();

        cleanedReadings.put(SensorType.RADAR, validateRadar(data));
        cleanedReadings.put(SensorType.LIDAR, validateLidar(data));
        cleanedReadings.put(SensorType.CAMERA, validateCamera(data));
        cleanedReadings.put(SensorType.WHEEL_SPEED, validateWheelSpeed(data));

        return new ProcessedSensorData(cleanedReadings, data.getTimestamp());
    }

    /**
     * Validates radar readings by checking garbage flags and distance
     * disagreement between primary and redundant sensors.
     * @param data the ProcessedSensorData containing radar readings
     * @return map of trustworthy radar readings, empty if both failed
     */
    private Map<SensorId, SensorData> validateRadar(ProcessedSensorData data) {
        RadarData primary = (RadarData) data.getSensorData(SensorType.RADAR, SensorId.PRIMARY);
        RadarData redundant = (RadarData) data.getSensorData(SensorType.RADAR, SensorId.REDUNDANT);

        return resolveDistanceSensor(primary, redundant, RADAR_DISTANCE_THRESHOLD);
    }

    /**
     * Validates lidar readings by checking garbage flags and distance
     * disagreement between primary and redundant sensors.
     * @param data the ProcessedSensorData containing lidar readings
     * @return map of trustworthy lidar readings, empty if both failed
     */
    private Map<SensorId, SensorData> validateLidar(ProcessedSensorData data) {
        LidarData primary = (LidarData) data.getSensorData(SensorType.LIDAR, SensorId.PRIMARY);
        LidarData redundant = (LidarData) data.getSensorData(SensorType.LIDAR, SensorId.REDUNDANT);

        return resolveDistanceSensor(primary, redundant, LIDAR_DISTANCE_THRESHOLD);
    }

    /**
     * Validates camera readings by checking garbage flags and confidence
     * disagreement between primary and redundant sensors.
     * @param data the ProcessedSensorData containing camera readings
     * @return map of trustworthy camera readings, empty if both failed
     */
    private Map<SensorId, SensorData> validateCamera(ProcessedSensorData data) {
        CameraData primary = (CameraData) data.getSensorData(SensorType.CAMERA, SensorId.PRIMARY);
        CameraData redundant = (CameraData) data.getSensorData(SensorType.CAMERA, SensorId.REDUNDANT);

        Map<SensorId, SensorData> result = new HashMap<>();

        if (primary == null && redundant == null) {return result;}
        if (primary == null || primary.isGarbage()) {
            if (redundant != null && !redundant.isGarbage()) {
                result.put(SensorId.PRIMARY, redundant);
            }
            return result;
        }
        if (redundant == null || redundant.isGarbage()) {
            result.put(SensorId.PRIMARY, primary);
            return result;
        }

        double confidenceDiff = Math.abs(
                primary.getConfidence() - redundant.getConfidence());

        if (confidenceDiff <= CAMERA_CONFIDENCE_THRESHOLD) {
            result.put(SensorId.PRIMARY, primary);
            result.put(SensorId.REDUNDANT, redundant);
        } else {
            result.put(SensorId.PRIMARY, primary);
        }

        return result;
    }

    /**
     * Validates wheel speed readings by checking garbage flags and average
     * wheel speed disagreement between primary and redundant sensors.
     * @param data the ProcessedSensorData containing wheel speed readings
     * @return map of trustworthy wheel speed readings, empty if both failed
     */
    private Map<SensorId, SensorData> validateWheelSpeed(ProcessedSensorData data) {
        WheelSpeedData primary = (WheelSpeedData) data.getSensorData(SensorType.WHEEL_SPEED, SensorId.PRIMARY);
        WheelSpeedData redundant = (WheelSpeedData) data.getSensorData(SensorType.WHEEL_SPEED, SensorId.REDUNDANT);

        Map<SensorId, SensorData> result = new HashMap<>();

        if (primary == null && redundant == null) {return result;}
        if (primary == null || primary.isGarbage()) {
            if (redundant != null && !redundant.isGarbage()) {
                result.put(SensorId.PRIMARY, redundant);
            }
            return result;
        }
        if (redundant == null || redundant.isGarbage()) {
            result.put(SensorId.PRIMARY, primary);
            return result;
        }

        double primaryAvgSpeed = average(primary.getWheelSpeeds());
        double redundantAvgSpeed = average(redundant.getWheelSpeeds());
        double speedDiff = Math.abs(primaryAvgSpeed - redundantAvgSpeed);

        if (speedDiff <= WHEEL_SPEED_THRESHOLD) {
            result.put(SensorId.PRIMARY, primary);
            result.put(SensorId.REDUNDANT, redundant);
        } else {
            result.put(SensorId.PRIMARY, primary);
        }

        return result;
    }

    /**
     * Shared resolution logic for distance-based sensors (Radar and Lidar).
     * Checks garbage flags first, then compares distance values against threshold.
     * Promotes redundant if primary is garbage.
     * @param primary   the primary sensor data, may be null
     * @param redundant the redundant sensor data, may be null
     * @param threshold maximum allowed distance difference in metres
     * @return map of trustworthy readings
     */
    private Map<SensorId, SensorData> resolveDistanceSensor(SensorData primary, SensorData redundant, double threshold) {
        Map<SensorId, SensorData> result = new HashMap<>();

        if (primary == null && redundant == null) {return result;}
        if (primary == null || primary.isGarbage()) {
            if (redundant != null && !redundant.isGarbage()) {
                result.put(SensorId.PRIMARY, redundant);
            }
            return result;
        }
        if (redundant == null || redundant.isGarbage()) {
            result.put(SensorId.PRIMARY, primary);
            return result;
        }

        double primaryDistance = getDistance(primary);
        double redundantDistance = getDistance(redundant);
        double diff = Math.abs(primaryDistance - redundantDistance);

        if (diff <= threshold) {
            result.put(SensorId.PRIMARY, primary);
            result.put(SensorId.REDUNDANT, redundant);
        } else {
            result.put(SensorId.PRIMARY, primary);
        }

        return result;
    }

    /**
     * Extracts the distance value from a distance-based SensorData object.
     * Handles both RadarData and LidarData.
     * @param data the SensorData to extract distance from
     * @return distance in metres, or 9999.0 if type is unrecognised
     */
    private double getDistance(SensorData data) {
        if (data instanceof RadarData) {return ((RadarData) data).getDistance();}
        if (data instanceof LidarData) {return ((LidarData) data).getDistance();}
        return 9999.0;
    }

    /**
     * Calculates the average of a double array.
     * Used to compare overall wheel speed between primary and redundant sensors.
     * @param values the array of values to average
     * @return the average value, or 0.0 if array is empty
     */
    private double average(double[] values) {
        if (values == null || values.length == 0) {return 0.0;}
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }
}