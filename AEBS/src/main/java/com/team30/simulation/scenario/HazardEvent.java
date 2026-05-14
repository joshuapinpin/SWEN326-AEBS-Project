package com.team30.simulation.scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team30.core.datalayer.enums.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HazardEvent {
    private long triggerTime;
    private HazardType type;
    private double worldPosition;
    private double objectSpeed;
    private ObjectType objectType;
    private MovementDirection movementDirection;
    private boolean inCurrentLane;
    private SensorType sensorType;
    private SensorId sensorId;
    private WeatherCondition newWeather;
    private LightCondition newLight;
    private boolean triggered;
    private int failureCount;

    // Default constructor required by Jackson
    public HazardEvent() {}

    public long getTriggerTime()                    { return triggerTime; }
    public HazardType getType()                     { return type; }
    public double getWorldPosition()                { return worldPosition; }
    public double getObjectSpeed()                  { return objectSpeed; }
    public ObjectType getObjectType()               { return objectType; }
    public MovementDirection getMovementDirection() { return movementDirection; }
    public boolean isInCurrentLane()                { return inCurrentLane; }
    public SensorType getSensorType()               { return sensorType; }
    public SensorId getSensorId()                   { return sensorId; }
    public WeatherCondition getNewWeather()         { return newWeather; }
    public LightCondition getNewLight()             { return newLight; }
    public boolean isTriggered()                    { return triggered; }
    public void setTriggered(boolean v)             { this.triggered = v; }
    public int getFailureCount() { return failureCount; }
}