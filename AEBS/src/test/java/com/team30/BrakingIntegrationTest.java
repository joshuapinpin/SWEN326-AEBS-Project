package com.team30;

import com.team30.core.datalayer.data.*;
import com.team30.core.datalayer.enums.*;
import com.team30.core.logic.BrakeSystemController;
import com.team30.core.logic.CollisionDetector;
import com.team30.simulation.state.CarState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BrakingIntegrationTest {

    private static final double WHEEL_CIRCUMFERENCE = 2.0;
    private static final long TICK_MS = 10;
    private static final double INITIAL_SPEED = 16.67;

    private static final double MIN_DETECTION_DISTANCE = 0.5;
    private static final double MAX_DETECTION_DISTANCE = 200.0;

    private CarState carState;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeController;

    private double pedestrianDistance;

    @BeforeEach
    void setUp() {
        double[] initialRPM = {500, 500, 500, 500};

        carState = new CarState(
                INITIAL_SPEED,
                INITIAL_SPEED,
                initialRPM,
                DrivingMode.CRUISING,
                0.0,
                2.0,
                WeatherCondition.CLEAR,
                LightCondition.DAY
        );

        collisionDetector = new CollisionDetector();
        brakeController = new BrakeSystemController(carState);
    }

    private ProcessedSensorData buildPedestrianDetected(
            double distance,
            double relativeSpeed,
            boolean inLane
    ) {
        long ts = carState.getCurrentTimeMs();

        boolean detected =
                distance >= MIN_DETECTION_DISTANCE
                        && distance <= MAX_DETECTION_DISTANCE;

        RadarData radar = new RadarData(
                SensorId.PRIMARY,
                ts,
                distance,
                relativeSpeed,
                detected
        );

        CameraData camera = new CameraData(
                SensorId.PRIMARY,
                ts,
                ObjectType.PEDESTRIAN,
                inLane,
                detected ? 0.9 : 0.0
        );

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        Map<SensorId, SensorData> cameraMap = new HashMap<>();

        radarMap.put(SensorId.PRIMARY, radar);
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        return new ProcessedSensorData(readings, ts);
    }

    private void simulateTick(double relativeSpeed) {
        carState.setCurrentTimeMs(carState.getCurrentTimeMs() + TICK_MS);

        double decel = carState.getDecelerationRate();

        if (decel > 0) {
            double newSpeed = Math.max(
                    0.0,
                    carState.getCarSpeed() - decel * (TICK_MS / 1000.0)
            );

            carState.setCarSpeed(newSpeed);

            double newRPM = (newSpeed / WHEEL_CIRCUMFERENCE) * 60.0;
            carState.setWheelRPM(new double[]{newRPM, newRPM, newRPM, newRPM});
        }

        double closingThisTick = relativeSpeed * (TICK_MS / 1000.0);

        pedestrianDistance = Math.max(
                0.0,
                pedestrianDistance - closingThisTick
        );
    }

    @Test
    void fullScenario_pedestrianDetected_warnsThenBrakesToStop() {
        pedestrianDistance = 210.0;
        double pedestrianSpeed = 0.0;

        boolean sawWarning = false;
        boolean sawBrake = false;
        boolean collisionOccurred = false;

        CollisionAssessment assessment = null;

        while (carState.getCarSpeed() > 0) {
            double relativeSpeed = carState.getCarSpeed() - pedestrianSpeed;

            simulateTick(relativeSpeed);

            ProcessedSensorData data = buildPedestrianDetected(
                    pedestrianDistance,
                    relativeSpeed,
                    true
            );

            assessment = collisionDetector.assess(data);
            assertNotNull(assessment);

            ThreatLevel threat = assessment.getThreatLevel();

            System.out.printf(
                    "t=%4dms | speed=%6.3f m/s | distance=%7.3fm | TTC=%6.2f | threat=%s | mode=%s%n",
                    carState.getCurrentTimeMs(),
                    carState.getCarSpeed(),
                    pedestrianDistance,
                    assessment.getTimeToCollision(),
                    threat,
                    carState.getDrivingMode()
            );

            if (threat == ThreatLevel.WARNING) {
                sawWarning = true;

                assertEquals(
                        DrivingMode.CRUISING,
                        carState.getDrivingMode(),
                        "WARNING should not activate braking yet"
                );
            }

            if (threat == ThreatLevel.BRAKE) {
                sawBrake = true;

                BrakeDecision decision = brakeController.execute(assessment);

                assertEquals(
                        BrakeResult.SUCCESS,
                        decision.getResult(),
                        "Brake command should succeed when braking starts"
                );

                assertEquals(
                        DrivingMode.BRAKING,
                        carState.getDrivingMode(),
                        "BRAKE threat should activate BRAKING mode"
                );

                assertEquals(
                        8.0,
                        carState.getDecelerationRate(),
                        0.001,
                        "BRAKING mode should apply hard deceleration"
                );
            }

            if (carState.getDrivingMode() == DrivingMode.BRAKING) {
                brakeController.execute(assessment);

                assertEquals(
                        DrivingMode.BRAKING,
                        carState.getDrivingMode(),
                        "Once braking starts, it should remain braking until stopped"
                );
            }

            if (pedestrianDistance <= 0.0) {
                collisionOccurred = true;
                break;
            }
        }

        assertNotNull(assessment);

        assertTrue(
                sawWarning,
                "System should enter WARNING before BRAKE"
        );

        assertTrue(
                sawBrake,
                "System should eventually enter BRAKE"
        );

        assertFalse(
                collisionOccurred,
                "Car should stop before hitting the pedestrian"
        );

        assertEquals(
                0.0,
                carState.getCarSpeed(),
                0.001,
                "Car should brake to a full stop"
        );

        assertEquals(
                DrivingMode.BRAKING,
                carState.getDrivingMode(),
                "Car should remain in BRAKING mode at the end of emergency stop"
        );

        System.out.printf(
                "Stopped safely with %.3fm remaining.%n",
                pedestrianDistance
        );
    }
}