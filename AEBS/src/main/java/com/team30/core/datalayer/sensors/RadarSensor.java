package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.RadarData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

import java.util.Random;

/**
 * Simulates a radar sensor that detects objects in front of the car.
 * It provides distance and relative speed information about the closest object within its range.
 * The sensor's accuracy is affected by weather conditions, introducing noise into the readings.
 */
public class RadarSensor extends Sensor {

    private static final double MAX_RANGE = 200.0;
    private static final double MIN_RANGE = 0.5;
    private static final int FIRE_EVERY = 10;

    private final Random random = new Random();

    /**
     * Constructor for RadarSensor.
     * @param sensorId Unique identifier for the sensor (e.g., PRIMARY, REDUNDANT).
     * @param carState The initial state of the car, which the sensor will use to generate readings.
     */
    public RadarSensor(SensorId sensorId, CarState carState) {
        super(sensorId, carState, FIRE_EVERY);
    }

    @Override
    public SensorData generateReading(CarState state) {
        WorldObject closest = findClosestInRange(state, MIN_RANGE, MAX_RANGE);

        if (closest == null) {
            return new RadarData(sensorId, state.getCurrentTimeMs(), -1.0, 0.0, false);
        }

        double noise = getPositionNoise(state.getWeather());
        double noisyDistance = closest.getPosition() + noise;
        double relativeSpeed = state.getCarSpeed() - closest.getSpeed();

        return new RadarData(sensorId, state.getCurrentTimeMs(), noisyDistance, relativeSpeed, true);
    }

    @Override
    public SensorData generateGarbageReading() {
        return new RadarData(sensorId, carState.getCurrentTimeMs());
    }

    @Override
    protected double getMaxRange() { return MAX_RANGE; }

    @Override
    public SensorType getSensorType() {
        return SensorType.RADAR;
    }

    private double getPositionNoise(WeatherCondition w) {
        double noiseRange = switch (w) {
            case CLEAR      -> 2.0;
            case CLOUDY     -> 3.0;
            case RAIN       -> 5.0;
            case HEAVY_RAIN -> 8.0;
            case FOG        -> 6.0;
            case SNOW       -> 7.0;
            case HEAVY_SNOW -> 9.0;
        };
        return (random.nextDouble() * 2.0 - 1.0) * noiseRange;
    }
}