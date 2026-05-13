package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.FaultHandler;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.state.CarState;

import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FaultHandlerTest {

    private static final Logger log =
            LogManager.getLogger(FaultHandlerTest.class);

    private CarState createCarState() {

        log.debug("Creating default CarState for tests");

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

        log.debug("Creating valid sensor data for all sensor types");

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

        log.info("STARTING: testExhaustedBrakeTriggersCriticalFailure");

        CarState carState = createCarState();
        DriverInterface driverInterface = new DriverInterface(carState);
        FaultHandler handler = new FaultHandler(driverInterface, carState);

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

        log.debug("Calling FaultHandler.handle() with EXHAUSTED brake result");

        handler.handle(decision, data);

        assertTrue(handler.hasCriticalFailure());
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode());

        log.info("ENDING: testExhaustedBrakeTriggersCriticalFailure");
    }

    @Test
    public void testTwoSensorFailuresTriggerCriticalFailure() {

        log.info("STARTING: testTwoSensorFailuresTriggerCriticalFailure");

        CarState carState = createCarState();
        DriverInterface driverInterface = new DriverInterface(carState);
        FaultHandler handler = new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                createAllValidSensors();

        log.debug("Simulating two sensor failures: RADAR and LIDAR");

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
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode());

        log.info("ENDING: testTwoSensorFailuresTriggerCriticalFailure");
    }

    @Test
    public void testOneSensorFailureTriggersCriticalFailure() {

        log.info("STARTING: testOneSensorFailureTriggersCriticalFailure");

        CarState carState = createCarState();
        DriverInterface driverInterface = new DriverInterface(carState);
        FaultHandler handler = new FaultHandler(driverInterface, carState);

        Map<SensorType, Map<SensorId, SensorData>> readings =
                createAllValidSensors();

        log.debug("Simulating one sensor failure: RADAR");

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
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode());

        log.info("ENDING: testOneSensorFailureTriggersCriticalFailure");
    }

    @Test
    public void testAllSensorsPresentNoCriticalFailure() {

        log.info("STARTING: testAllSensorsPresentNoCriticalFailure");

        CarState carState = createCarState();
        DriverInterface driverInterface = new DriverInterface(carState);
        FaultHandler handler = new FaultHandler(driverInterface, carState);

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

        log.debug("Calling FaultHandler.handle() with all sensors valid");

        handler.handle(decision, data);

        assertFalse(handler.hasCriticalFailure());
        assertEquals(DrivingMode.CRUISING, carState.getDrivingMode());

        log.info("ENDING: testAllSensorsPresentNoCriticalFailure");
    }
}
