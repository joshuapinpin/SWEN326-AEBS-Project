package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.LidarData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

import java.util.Random;

public class LidarSensor extends Sensor {

    private static final double MAX_RANGE  = 200.0;
    private static final double MIN_RANGE  = 0.5;
    private static final int    FIRE_EVERY = 10;

    private final Random random = new Random();

    public LidarSensor(SensorId sensorId, CarState carState) {
        super(sensorId, carState, FIRE_EVERY);
    }

    @Override
    public SensorData generateReading(CarState state) {
        WorldObject closest = findClosestInRange(state, MIN_RANGE, MAX_RANGE);

        if (closest == null) {
            return new LidarData(sensorId, state.getCurrentTimeMs(), -1.0, 0.0, false);
        }

        double noise = getPositionNoise(state.getWeather());
        double noisyDistance = closest.getPosition() + noise;
        double relativeSpeed = state.getCarSpeed() - closest.getSpeed();

        return new LidarData(sensorId, state.getCurrentTimeMs(), noisyDistance, relativeSpeed, true);
    }

    @Override
    public SensorData generateGarbageReading() {
        return new LidarData(sensorId, carState.getCurrentTimeMs());
    }

    @Override
    protected double getMaxRange() { return MAX_RANGE; }

    @Override
    public SensorType getSensorType() {
        return SensorType.LIDAR;
    }

    private double getPositionNoise(WeatherCondition w) {
        double noiseRange = switch (w) {
            case CLEAR      -> 0.5;
            case CLOUDY     -> 0.8;
            case RAIN       -> 1.0;
            case HEAVY_RAIN -> 2.0;
            case FOG        -> 8.0;
            case SNOW       -> 4.0;
            case HEAVY_SNOW -> 7.0;
        };
        return (random.nextDouble() * 2.0 - 1.0) * noiseRange;
    }
}