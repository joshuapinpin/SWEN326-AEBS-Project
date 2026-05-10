package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.CameraData;
import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.*;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

public class CameraSensor extends Sensor {

    private static final int    FIRE_EVERY           = 5;
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    public CameraSensor(SensorId sensorId, CarState carState) {
        super(sensorId, carState, FIRE_EVERY);
    }

    @Override
    public SensorData generateReading(CarState state) {
        double confidence = calculateConfidence(state.getWeather(), state.getLight());
        WorldObject closest = findClosestInRange(state, 0.0, getMaxRange());

        ObjectType classification;
        boolean inLane;

        if (closest == null || confidence < CONFIDENCE_THRESHOLD) {
            classification = ObjectType.UNKNOWN;
            inLane = false;
        } else {
            classification = closest.getType();
            inLane = closest.isInCurrentLane();
        }

        return new CameraData(sensorId, state.getCurrentTimeMs(), classification, inLane, confidence);
    }

    @Override
    public SensorData generateGarbageReading() {
        return new CameraData(sensorId, carState.getCurrentTimeMs());
    }

    @Override
    public SensorType getSensorType() {
        return SensorType.CAMERA;
    }

    private double calculateConfidence(WeatherCondition w, LightCondition l) {
        double weatherFactor = switch (w) {
            case CLEAR      -> 1.0;
            case CLOUDY     -> 0.85;
            case RAIN       -> 0.65;
            case HEAVY_RAIN -> 0.4;
            case FOG        -> 0.3;
            case SNOW       -> 0.5;
            case HEAVY_SNOW -> 0.25;
        };

        double lightFactor = switch (l) {
            case DAY -> 1.0;
            case DUSK  -> 0.65;
            case NIGHT      -> 0.4;
        };

        return Math.min(1.0, weatherFactor * lightFactor);
    }
}