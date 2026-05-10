package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.data.WheelSpeedData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.core.datalayer.enums.SensorType;
import com.team30.simulation.state.CarState;

import java.util.Random;

public class WheelSpeedSensor extends Sensor {

    private static final int    FIRE_EVERY          = 1;
    private static final double WHEEL_CIRCUMFERENCE = 2.0;
    private static final double NOISE_RANGE         = 5.0;

    private final Random random = new Random();

    public WheelSpeedSensor(SensorId sensorId, CarState carState) {
        super(sensorId, carState, FIRE_EVERY);
    }

    @Override
    public SensorData generateReading(CarState state) {
        double[] rawRPM    = state.getWheelRPM();
        double[] noisyRPM  = new double[4];
        double[] wheelSpeeds = new double[4];

        for (int i = 0; i < 4; i++) {
            double noise  = (random.nextDouble() * 2.0 - 1.0) * NOISE_RANGE;
            noisyRPM[i]   = rawRPM[i] + noise;
            wheelSpeeds[i] = noisyRPM[i] * WHEEL_CIRCUMFERENCE / 60.0;
        }

        return new WheelSpeedData(sensorId, state.getCurrentTimeMs(), noisyRPM, wheelSpeeds);
    }

    @Override
    public SensorData generateGarbageReading() {
        return new WheelSpeedData(sensorId, carState.getCurrentTimeMs());
    }

    @Override
    public SensorType getSensorType() {
        return SensorType.WHEEL_SPEED;
    }
}