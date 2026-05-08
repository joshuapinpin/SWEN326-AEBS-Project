package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.data.SensorData;
import com.team30.core.datalayer.enums.SensorId;
import com.team30.simulation.state.CarState;


public class RadarSensor extends Sensor {

    protected RadarSensor(SensorId sensorId, CarState carState, int fireEvery) {
        super(sensorId, carState, fireEvery);
    }

    @Override
    public SensorData generateReading(CarState state) {
        return null;
    }

    @Override
    public SensorData generateGarbageReading() {
        return null;
    }
}