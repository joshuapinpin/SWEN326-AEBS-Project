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

    private ProcessedSensorData createEmptyData() {
        return new ProcessedSensorData(
                new HashMap<>(),
                System.currentTimeMillis()
        );
    }

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

    @Test
    public void testExhaustedBrakeTriggersFailSafe() {

        CarState carState = createCarState();

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

        handler.handle(decision, createEmptyData());

        assertEquals(
                DrivingMode.FAIL_SAFE,
                carState.getDrivingMode()
        );

        assertEquals(
                0.0,
                carState.getTargetSpeed()
        );
    }

    @Test
    public void testTwoSensorFailuresTriggerFailSafe() {

        CarState carState = createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        readings.put(SensorType.RADAR, new HashMap<>());
        readings.put(SensorType.LIDAR, new HashMap<>());

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(false, 0, BrakeResult.NOT_NEEDED, 0);

        handler.handle(decision, data);

        assertEquals(
                DrivingMode.FAIL_SAFE,
                carState.getDrivingMode()
        );
    }

    @Test
    public void testOneSensorFailureShowsWarning() {

        CarState carState = createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        readings.put(SensorType.RADAR, new HashMap<>());

        Map<SensorId, SensorData> lidarMap = new HashMap<>();

        lidarMap.put(
                SensorId.PRIMARY,
                new LidarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        20,
                        5,
                        true
                )
        );

        readings.put(SensorType.LIDAR, lidarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(false, 0, BrakeResult.NOT_NEEDED, 0);

        handler.handle(decision, data);

        assertNotEquals(
                DrivingMode.FAIL_SAFE,
                carState.getDrivingMode()
        );
    }

    @Test
    public void testNoSensorFailureNoAction() {

        CarState carState = createCarState();

        DriverInterface driverInterface =
                new DriverInterface(carState);

        FaultHandler handler =
                new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();

        Map<SensorId, SensorData> radarMap = new HashMap<>();

        radarMap.put(
                SensorId.PRIMARY,
                new RadarData(
                        SensorId.PRIMARY,
                        System.currentTimeMillis(),
                        10,
                        5,
                        true
                )
        );

        readings.put(SensorType.RADAR, radarMap);

        ProcessedSensorData data =
                new ProcessedSensorData(
                        readings,
                        System.currentTimeMillis()
                );

        BrakeDecision decision =
                new BrakeDecision(false, 0, BrakeResult.NOT_NEEDED, 0);

        handler.handle(decision, data);

        assertEquals(
                DrivingMode.CRUISING,
                carState.getDrivingMode()
        );
    }
}