package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;

/**
 * Represents the wheel speed data from the vehicle's sensors.
 * Includes both the RPM and the derived wheel speeds in m/s for each wheel.
 */
public class WheelSpeedData extends SensorData {
    private final double[] rpm;          // [frontLeft, frontRight, rearLeft, rearRight]
    private final double[] wheelSpeeds;  // m/s per wheel, derived from RPM

    /**
     * Constructs a WheelSpeedData with the given sensor ID, timestamp, RPM values, and derived wheel speeds.
     * @param sensorId the ID of the sensor that produced this reading (PRIMARY or REDUNDANT)
     * @param timestampMs the time this reading was captured in milliseconds since epoch
     * @param rpm an array of RPM values for each wheel in the order: [frontLeft, frontRight, rearLeft, rearRight]
     * @param wheelSpeeds array of wheel speeds in m/s for each wheel, derived from the RPM values, in the same order
     */
    public WheelSpeedData(SensorId sensorId, long timestampMs, double[] rpm, double[] wheelSpeeds) {
        super(sensorId, timestampMs, false);
        this.rpm = rpm;
        this.wheelSpeeds = wheelSpeeds;
    }

    /** Garbage constructor */
    public WheelSpeedData(SensorId sensorId, long timestampMs) {
        super(sensorId, timestampMs, true);
        this.rpm = new double[]{-9999.0, -9999.0, -9999.0, -9999.0};
        this.wheelSpeeds = new double[]{-9999.0, -9999.0, -9999.0, -9999.0};
    }

    /** Getters for RPM and wheel speeds. */
    public double[] getRpm() { return rpm; }
    public double[] getWheelSpeeds() { return wheelSpeeds; }

    public double getFrontLeftRpm() { return rpm[0]; }
    public double getFrontRightRpm() { return rpm[1]; }
    public double getRearLeftRpm() { return rpm[2]; }
    public double getRearRightRpm() { return rpm[3]; }

    public double getFrontLeftSpeed() { return wheelSpeeds[0]; }
    public double getFrontRightSpeed() { return wheelSpeeds[1]; }
    public double getRearLeftSpeed() { return wheelSpeeds[2]; }
    public double getRearRightSpeed() { return wheelSpeeds[3]; }

    @Override
    public SensorType getSensorType() { return SensorType.WHEEL_SPEED; }
}