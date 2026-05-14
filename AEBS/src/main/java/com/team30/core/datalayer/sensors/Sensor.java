package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.core.datalayer.observers.SensorObserver;
import com.team30.core.datalayer.observers.SensorSubject;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Abstract base class for all sensors in the AEBS system.
 * Implements common functionality for managing observers, generating readings,
 * and calculating detection probabilities based on weather and distance.
 */
public abstract class Sensor implements SensorSubject {
    boolean sensorFailed;
    boolean working;
    SensorId sensorId;
    CarState carState;
    int tickCount;
    int fireEvery;
    List<SensorObserver> observers;

    /**
     * Constructor for Sensor.
     * @param sensorId Unique identifier for the sensor (e.g., PRIMARY, REDUNDANT).
     * @param carState Initial state of the car, which the sensor will use to generate readings.
     * @param fireEvery Number of ticks between each sensor reading
     */
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
     * Generates a sensor data reading based on the current CarState.
     * @param state The latest CarState to use for generating the sensor reading.
     * @return A SensorData object containing the reading information.
     */
    public abstract SensorData generateReading(CarState state);

    /**
     * Generates a "garbage" sensor reading when the sensor has failed or is not working.
     * @return A SensorData object containing invalid or default values to indicate a failed reading.
     */
    public abstract SensorData generateGarbageReading();

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

    /**
     * Finds the closest WorldObject within a specified range from the car's current position.
     * @param state The CarState containing the current position and list of WorldObjects in the environment.
     * @param minRange Minimum distance from the car to consider (inclusive).
     * @param maxRange Maximum distance from the car to consider (inclusive).
     * @return The closest WorldObject within the specified range, or null if no such object exists.
     */
    public WorldObject findClosestInRange(CarState state, double minRange, double maxRange) {
        List<WorldObject> objects = state.getObjectsInWorld();
        if (objects == null || objects.isEmpty()) return null;

        return objects.stream()
                .filter(obj -> obj.getPosition() >= minRange && obj.getPosition() <= maxRange)
                .min(Comparator.comparingDouble(WorldObject::getPosition))
                .orElse(null);
    }

    @Override
    public void attachObserver(SensorObserver o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
        }
    }

    @Override
    public void detachObserver(SensorObserver o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(SensorData data) {
        for (SensorObserver observer : observers) {
            observer.update(data);
        }
    }

    /** Getters and setters */
    protected double getMaxRange() {return 100.0;}
    public CarState getCarState() { return carState; }
    public void setCarState(CarState carState) { this.carState = carState; }
    public SensorId getSensorId() { return sensorId; }
    public void setWorking(boolean v) { this.working = v; }
    public abstract SensorType getSensorType();
}