package com.team30.simulation.scenario;
import com.team30.core.datalayer.enums.HazardType;
import com.team30.core.datalayer.enums.ObjectType;
import com.team30.core.datalayer.enums.MovementDirection;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.core.datalayer.enums.LightCondition;

public class HazardEvent {
    long triggerTime;
    HazardType type;
    double worldPosition;
    double objectSpeed;
    ObjectType objectType;
    MovementDirection movementDirection;
    boolean inCurrentLane;
    SensorType sensorType;
    SensorId sensorId;
    boolean working;
    WeatherCondition newWeather;
    LightCondition newLight;
    boolean triggered;

    public long getTriggerTime() {
        return triggerTime;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public HazardType getType() {
        return type;
    }
}
