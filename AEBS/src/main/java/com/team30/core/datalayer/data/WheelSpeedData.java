package com.team30.core.datalayer.data;

import com.team30.core.datalayer.enums.SensorId;

public class WheelSpeedData extends SensorData {
    private final double[] rpm;          // [frontLeft, frontRight, rearLeft, rearRight]
    private final double[] wheelSpeeds;  // m/s per wheel, derived from RPM

    public WheelSpeedData(SensorId sensorId, long timestampMs, double[] rpm, double[] wheelSpeeds) {
        super(sensorId, timestampMs, false);
        this.rpm = rpm;
        this.wheelSpeeds = wheelSpeeds;
    }

    /** Garbage constructor */
    public WheelSpeedData(SensorId sensorId, long timestampMs) {
        super(sensorId, timestampMs, true);
        this.rpm         = new double[]{-9999.0, -9999.0, -9999.0, -9999.0};
        this.wheelSpeeds = new double[]{-9999.0, -9999.0, -9999.0, -9999.0};
    }

    public double[] getRpm()         { return rpm; }
    public double[] getWheelSpeeds() { return wheelSpeeds; }

    public double getFrontLeftRpm()  { return rpm[0]; }
    public double getFrontRightRpm() { return rpm[1]; }
    public double getRearLeftRpm()   { return rpm[2]; }
    public double getRearRightRpm()  { return rpm[3]; }

    public double getFrontLeftSpeed()  { return wheelSpeeds[0]; }
    public double getFrontRightSpeed() { return wheelSpeeds[1]; }
    public double getRearLeftSpeed()   { return wheelSpeeds[2]; }
    public double getRearRightSpeed()  { return wheelSpeeds[3]; }
}