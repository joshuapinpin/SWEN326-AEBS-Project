package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
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
     * Called every simulation tick. Increments tickCount and fires a reading
     * when tickCount % fireEvery == 0.
     * If the sensor has failed or is toggled off, generates a garbage reading instead.
     */
    public void onTick(long time) {
        tickCount++;
        if (tickCount % fireEvery == 0) {
            SensorData data;
            if (sensorFailed || !working) {
                data = generateGarbageReading();
            } else {
                data = generateReading(carState);
            }
            notifyObservers(data);
        }
    }

    /**
     * Registers a SensorObserver to receive data from this sensor.
     */
    public void attach(SensorObserver o) {
        if (o != null && !observers.contains(o)) {
            observers.add(o);
        }
    }

    /**
     * Removes a previously registered SensorObserver.
     */
    public void detach(SensorObserver o) {
        observers.remove(o);
    }

    /**
     * Notifies all registered observers with the latest SensorData.
     */
    public void notifyObservers(SensorData data) {
        for (SensorObserver observer : observers) {
            //observer.update(data);
        }
    }

    /**
     * Generates a valid sensor reading from the current car state.
     */
    public abstract SensorData generateReading(CarState state);

    /**
     * Generates a garbage/noise reading to simulate sensor failure.
     */
    public abstract SensorData generateGarbageReading();

    /**
     * Finds the closest WorldObject within a given distance range from the car's position.
     *
     * @param state    The current car state (used to get the car's position and nearby objects)
     * @param minRange Minimum detection range (objects closer than this are ignored)
     * @param maxRange Maximum detection range (objects further than this are ignored)
     * @return The closest WorldObject within range, or null if none found
     */
    public WorldObject findClosestInRange(CarState state, double minRange, double maxRange) {
        List<WorldObject> nearbyObjects = state.getWorldObjects();
        if (nearbyObjects == null || nearbyObjects.isEmpty()) {
            return null;
        }

        return nearbyObjects.stream()
                .filter(obj -> {
                    double distance = state.getPosition().distanceTo(obj.getPosition());
                    return distance >= minRange && distance <= maxRange;
                })
                .min(Comparator.comparingDouble(
                        obj -> state.getPosition().distanceTo(obj.getPosition())
                ))
                .orElse(null);
    }

    /**
     * Returns the probability (0.0 to 1.0) that this sensor detects an object
     * at a given distance under a given weather condition.
     *
     * Probability degrades with distance and adverse weather.
     *
     * @param w        The current weather condition
     * @param distance Distance to the object in metres
     * @return Detection probability between 0.0 and 1.0
     */
    public double getDetectionProbability(WeatherCondition w, double distance) {
        // Base probability falls off linearly with distance up to maxRange
        double maxRange = getMaxRange();
        if (distance > maxRange || distance < 0) {
            return 0.0;
        }

        double distanceFactor = 1.0 - (distance / maxRange);

        // Apply weather degradation multiplier
        double weatherMultiplier = switch (w) {
            case CLEAR       -> 1.0;
            case CLOUDY      -> 0.9;
            case RAIN        -> 0.7;
            case HEAVY_RAIN  -> 0.5;
            case FOG         -> 0.4;
            case SNOW        -> 0.5;
            case HEAVY_SNOW  -> 0.3;
            default          -> 1.0;
        };

        return Math.max(0.0, Math.min(1.0, distanceFactor * weatherMultiplier));
    }

    /**
     * Returns the maximum effective detection range for this sensor type.
     * Subclasses should override this to return their specific range.
     */
    protected double getMaxRange() {
        return 100.0; // default 100 metres; override in subclasses
    }

    // --- Getters / Setters ---

    public SensorId getSensorId() { return sensorId; }

    public boolean isSensorFailed() { return sensorFailed; }
    public void setSensorFailed(boolean sensorFailed) { this.sensorFailed = sensorFailed; }

    public boolean isWorking() { return working; }
    public void setWorking(boolean working) { this.working = working; }

    public int getTickCount() { return tickCount; }
    public int getFireEvery() { return fireEvery; }
}