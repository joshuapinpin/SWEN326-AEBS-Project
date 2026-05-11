package com.team30.core.logic;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FaultHandlerTest {

    private CarState createCarState() {
        return new CarState(
                50,
                50,
                new double[]{100, 100, 100, 100},
                DrivingMode.CRUISING,
                0,
                0,
                WeatherCondition.CLEAR,
                LightCondition.DAY
        );
    }

    /**
     * Creates valid sensor data for all sensor types.
     */
    private Map<SensorType, Map<SensorId, SensorData>> createAllValidSensors() {

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        for (SensorType type : SensorType.values()) {

            Map<SensorId, SensorData> sensorMap =
                    new HashMap<>();

            SensorData data;

            switch (type) {

                case RADAR:
                    data = new RadarData(
                            SensorId.PRIMARY,
                            System.currentTimeMillis(),
                            20,
                            5,
                            false
                    );
                    break;

                case LIDAR:
                    data = new LidarData(
                            SensorId.PRIMARY,
                            System.currentTimeMillis(),
                            20,
                            5,
                            false
                    );
                    break;

                default:
                    data = new RadarData(
                            SensorId.PRIMARY,
                            System.currentTimeMillis(),
                            20,
                            5,
                            false
                    );
            }

            sensorMap.put(SensorId.PRIMARY, data);
            readings.put(type, sensorMap);
        }

        return readings;
    }

    @Test
    public void testExhaustedBrakeTriggersCriticalFailure() {

        CarState carState =
                createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        BrakeDecision decision =
                new BrakeDecision(
                        true,
                        8.0,
                        BrakeResult.EXHAUSTED,
                        3
                );

        ProcessedSensorData data =
                new ProcessedSensorData(
                        createAllValidSensors(),
                        System.currentTimeMillis()
                );

        handler.handle(decision, data);

        assertTrue(handler.hasCriticalFailure());

        // FaultHandler does NOT modify driving mode
        assertEquals(
                DrivingMode.CRUISING,
                carState.getDrivingMode()
        );
    }

    @Test
    public void testTwoSensorFailuresTriggerCriticalFailure() {

        CarState carState =
                createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                createAllValidSensors();

        // Simulate failures
        readings.put(SensorType.RADAR, new HashMap<>());
        readings.put(SensorType.LIDAR, new HashMap<>());

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(
                        false,
                        0,
                        BrakeResult.NOT_NEEDED,
                        0
                );

        handler.handle(decision, data);

        assertTrue(handler.hasCriticalFailure());

        assertEquals(
                DrivingMode.CRUISING,
                carState.getDrivingMode()
        );
    }

    @Test
    public void testOneSensorFailureTriggersCriticalFailure() {

        CarState carState =
                createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                createAllValidSensors();

        // One sensor unavailable
        readings.put(SensorType.RADAR, new HashMap<>());

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(
                        false,
                        0,
                        BrakeResult.NOT_NEEDED,
                        0
                );

        handler.handle(decision, data);

        assertTrue(handler.hasCriticalFailure());

        assertEquals(
                DrivingMode.CRUISING,
                carState.getDrivingMode()
        );
    }

    @Test
    public void testAllSensorsPresentNoCriticalFailure() {

        CarState carState =
                createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        createAllValidSensors(),
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(
                        false,
                        0,
                        BrakeResult.NOT_NEEDED,
                        0
                );

        handler.handle(decision, data);

        assertFalse(handler.hasCriticalFailure());

        assertEquals(
                DrivingMode.CRUISING,
                carState.getDrivingMode()
        );
    }
}