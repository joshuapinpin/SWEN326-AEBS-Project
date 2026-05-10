package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.core.datalayer.observers.SensorObserver;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Sensor {

    boolean sensorFailed;
    boolean working;
    SensorId sensorId;
    CarState carState;
    int tickCount;
    int fireEvery;
    List<SensorObserver> observers;

    protected Sensor(SensorId sensorId, CarState carState, int fireEvery) {
        this.sensorId = sensorId;
        this.carState = carState;
        this.fireEvery = fireEvery;
        this.tickCount = 0;
        this.sensorFailed = false;
        this.working = true;
        this.observers = new ArrayList<>();
    }

    /**
     * Called every simulation tick with the latest CarState.
     * Updates internal state, increments tickCount, and fires a reading
     * when tickCount % fireEvery == 0, then resets tickCount to avoid overflow.
     */
    public void onTick(long time, CarState updatedState) {
        this.carState = updatedState;
        tickCount++;
        if (tickCount % fireEvery == 0) {
            SensorData data = (sensorFailed || !working)
                    ? generateGarbageReading()
                    : generateReading(carState);
            notifyObservers(data);
            tickCount = 0;
        }
    }

    public void attach(SensorObserver o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
        }
    }

    public void detach(SensorObserver o) {
        observers.remove(o);
    }

    public void notifyObservers(SensorData data) {
        for (SensorObserver observer : observers) {
            observer.update(data);
        }
    }

    public abstract SensorData generateReading(CarState state);
    public abstract SensorData generateGarbageReading();

    public WorldObject findClosestInRange(CarState state, double minRange, double maxRange) {
        List<WorldObject> objects = state.getObjectsInWorld();
        if (objects == null || objects.isEmpty()) return null;

        return objects.stream()
                .filter(obj -> obj.getPosition() >= minRange && obj.getPosition() <= maxRange)
                .min(Comparator.comparingDouble(WorldObject::getPosition))
                .orElse(null);
    }

    public double getDetectionProbability(WeatherCondition w, double distance) {
        double maxRange = getMaxRange();
        if (distance > maxRange || distance < 0) return 0.0;

        double distanceFactor = 1.0 - (distance / maxRange);

        double weatherMultiplier = switch (w) {
            case CLEAR      -> 1.0;
            case CLOUDY     -> 0.9;
            case RAIN       -> 0.7;
            case HEAVY_RAIN -> 0.5;
            case FOG        -> 0.4;
            case SNOW       -> 0.6;
            case HEAVY_SNOW -> 0.3;
        };

        return Math.max(0.0, Math.min(1.0, distanceFactor * weatherMultiplier));
    }

    protected double getMaxRange() {
        return 100.0;
    }

    /**
     * Convenience overload — uses the sensor's internally stored CarState.
     * Call this from subclasses when you don't need to pass state explicitly.
     */
    public WorldObject findClosestInRange(double minRange, double maxRange) {
        return findClosestInRange(this.carState, minRange, maxRange);
    }

    public CarState getCarState()                  { return carState; }
    public void setCarState(CarState carState)      { this.carState = carState; }
    public SensorId getSensorId()                  { return sensorId; }
    public boolean isSensorFailed()                { return sensorFailed; }
    public void setSensorFailed(boolean v)         { this.sensorFailed = v; }
    public boolean isWorking()                     { return working; }
    public void setWorking(boolean v)              { this.working = v; }
    public int getTickCount()                      { return tickCount; }
    public int getFireEvery()                      { return fireEvery; }
    public abstract SensorType getSensorType();
}