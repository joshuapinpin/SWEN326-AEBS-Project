package com.team30.simulation.state;

import com.team30.core.datalayer.enums.DrivingMode;
import com.team30.core.datalayer.enums.LightCondition;
import com.team30.core.datalayer.enums.WeatherCondition;


import java.util.*;

public class CarState {
    double carSpeed;
    double targetSpeed;
    double[] wheelRPM;          // [frontLeft, frontRight, rearLeft, rearRight]
    DrivingMode drivingMode;
    double decelerationRate;
    double accelerationRate;
    List<WorldObject> objectsInWorld;
    WeatherCondition weather;
    LightCondition light;
    boolean primaryRadarFailed;
    boolean redundantRadarFailed;
    boolean primaryLidarFailed;
    boolean redundantLidarFailed;
    boolean primaryCameraFailed;
    boolean redundantCameraFailed;
    boolean primaryWheelFailed;
    boolean redundantWheelFailed;
    long currentTimeMs;
    private double actualDeceleration;

    public CarState(double carSpeed, double targetSpeed, double[] wheelRPM,
                    DrivingMode drivingMode, double decelerationRate, double accelerationRate,
                    WeatherCondition weather, LightCondition light) {
        this.carSpeed = carSpeed;
        this.targetSpeed = targetSpeed;
        this.wheelRPM = (wheelRPM != null) ? wheelRPM : new double[4];
        this.drivingMode = drivingMode;
        this.decelerationRate = decelerationRate;
        this.accelerationRate = accelerationRate;
        this.weather = weather;
        this.light = light;
        this.objectsInWorld = new ArrayList<>();
        this.currentTimeMs = System.currentTimeMillis();
    }

    // --- Sensor failure flags ---
    public boolean isPrimaryRadarFailed()    { return primaryRadarFailed; }
    public boolean isRedundantRadarFailed()  { return redundantRadarFailed; }
    public boolean isPrimaryLidarFailed()    { return primaryLidarFailed; }
    public boolean isRedundantLidarFailed()  { return redundantLidarFailed; }
    public boolean isPrimaryCameraFailed()    { return primaryCameraFailed; }
    public boolean isRedundantCameraFailed() { return redundantCameraFailed; }
    public boolean isPrimaryWheelFailed()    { return primaryWheelFailed; }
    public boolean isRedundantWheelFailed()  { return redundantWheelFailed; }

    public void setPrimaryRadarFailed(boolean v)    { this.primaryRadarFailed = v; }
    public void setRedundantRadarFailed(boolean v)  { this.redundantRadarFailed = v; }
    public void setPrimaryLidarFailed(boolean v)    { this.primaryLidarFailed = v; }
    public void setRedundantLidarFailed(boolean v)  { this.redundantLidarFailed = v; }
    public void setPrimaryCameraFailed(boolean v)   { this.primaryCameraFailed = v; }
    public void setRedundantCameraFailed(boolean v) { this.redundantCameraFailed = v; }
    public void setPrimaryWheelFailed(boolean v)    { this.primaryWheelFailed = v; }
    public void setRedundantWheelFailed(boolean v)  { this.redundantWheelFailed = v; }

    // --- Core getters/setters ---
    public double getCarSpeed()            { return carSpeed; }
    public void setCarSpeed(double v)      { this.carSpeed = v; }

    public double getTargetSpeed()         { return targetSpeed; }
    public void setTargetSpeed(double v)   { this.targetSpeed = v; }

    public double[] getWheelRPM()          { return wheelRPM; }
    public void setWheelRPM(double[] v)    { this.wheelRPM = v; }

    public DrivingMode getDrivingMode()            { return drivingMode; }
    public void setDrivingMode(DrivingMode v)      { this.drivingMode = v; }

    public double getDecelerationRate()            { return decelerationRate; }
    public void setDecelerationRate(double v)      { this.decelerationRate = v; }

    public double getAccelerationRate()            { return accelerationRate; }
    public void setAccelerationRate(double v)      { this.accelerationRate = v; }

    public WeatherCondition getWeather()           { return weather; }
    public void setWeather(WeatherCondition v)     { this.weather = v; }

    public LightCondition getLight()               { return light; }
    public void setLight(LightCondition v)         { this.light = v; }

    public long getCurrentTimeMs()                 { return currentTimeMs; }
    public void setCurrentTimeMs(long v)           { this.currentTimeMs = v; }

    public List<WorldObject> getObjectsInWorld()           { return objectsInWorld; }
    public void setObjectsInWorld(List<WorldObject> list)  { this.objectsInWorld = list; }

    public double getActualDeceleration() { return actualDeceleration;}
    public void setActualDeceleration(double actualDeceleration) { this.actualDeceleration = actualDeceleration; }
}
