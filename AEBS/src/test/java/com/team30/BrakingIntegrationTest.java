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
    private static final long   TICK_MS             = 10;
    private static final double INITIAL_SPEED       = 16.67;

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
        brakeController   = new BrakeSystemController(carState);
    }

    private ProcessedSensorData buildPedestrianDetected(double distance,
                                                        double relativeSpeed,
                                                        boolean inLane) {

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

        Map<SensorType, Map<SensorId, SensorData>> readings =
                new HashMap<>();

        readings.put(SensorType.RADAR, radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        return new ProcessedSensorData(readings, ts);
    }

    private void simulateTick(double relativeSpeed) {

        carState.setCurrentTimeMs(
                carState.getCurrentTimeMs() + TICK_MS);

        double decel = carState.getDecelerationRate();

        if (decel > 0) {

            double newSpeed = Math.max(
                    0.0,
                    carState.getCarSpeed()
                            - decel * (TICK_MS / 1000.0)
            );

            carState.setCarSpeed(newSpeed);

            double newRPM =
                    (newSpeed / WHEEL_CIRCUMFERENCE) * 60.0;

            if (decel >= 8.0) {
                carState.setWheelRPM(
                        new double[]{newRPM, newRPM, 0.0, 0.0}
                );
            } else {
                carState.setWheelRPM(
                        new double[]{newRPM, newRPM, newRPM, newRPM}
                );
            }
        }

        double closingThisTick =
                relativeSpeed * (TICK_MS / 1000.0);

        pedestrianDistance = Math.max(
                0.0,
                pedestrianDistance - closingThisTick
        );
    }

    @Test
    void fullScenario_pedestrianDetected_carBrakesToStop() {

        pedestrianDistance = 210.0;

        double pedestrianSpeed = 0.0;

        System.out.println(
                "=== PEDESTRIAN DETECTION + BRAKING SCENARIO ===");

        System.out.printf(
                "Initial speed: %.2f m/s (%.1f km/h)%n",
                carState.getCarSpeed(),
                carState.getCarSpeed() * 3.6
        );

        System.out.printf(
                "Initial pedestrian distance: %.2fm%n",
                pedestrianDistance
        );

        System.out.println(
                "\n--- APPROACHING PEDESTRIAN ---");

        boolean detected = false;

        CollisionAssessment assessment = null;

        while (!detected) {

            double relativeSpeed =
                    carState.getCarSpeed() - pedestrianSpeed;

            simulateTick(relativeSpeed);

            ProcessedSensorData data =
                    buildPedestrianDetected(
                            pedestrianDistance,
                            relativeSpeed,
                            true
                    );

            RadarData radar =
                    (RadarData) data.getSensorData(
                            SensorType.RADAR,
                            SensorId.PRIMARY
                    );

            System.out.printf(
                    "t=%4dms | distance=%7.2fm | detected=%b%n",
                    carState.getCurrentTimeMs(),
                    pedestrianDistance,
                    radar.isObjectDetected()
            );

            if (radar.isObjectDetected()) {

                detected = true;

                System.out.println(
                        "\n*** PEDESTRIAN ENTERED SENSOR RANGE ***");

                assessment = collisionDetector.assess(data);

                System.out.printf(
                        "ThreatLevel=%s | TTC=%.2fs%n",
                        assessment.getThreatLevel(),
                        assessment.getTimeToCollision()
                );

                assertNotNull(assessment);
            }
        }

        assertNotNull(assessment);

        System.out.println(
                "\n--- WAITING FOR BRAKE THRESHOLD ---");

        while (assessment.getThreatLevel()
                != ThreatLevel.BRAKE) {

            double relativeSpeed =
                    carState.getCarSpeed() - pedestrianSpeed;

            simulateTick(relativeSpeed);

            ProcessedSensorData data =
                    buildPedestrianDetected(
                            pedestrianDistance,
                            relativeSpeed,
                            true
                    );

            assessment = collisionDetector.assess(data);

            System.out.printf(
                    "t=%4dms | distance=%7.2fm | TTC=%5.2fs | threat=%s%n",
                    carState.getCurrentTimeMs(),
                    pedestrianDistance,
                    assessment.getTimeToCollision(),
                    assessment.getThreatLevel()
            );
        }

        System.out.println(
                "\n*** BRAKE THRESHOLD REACHED ***");

        System.out.printf(
                "Distance: %.2fm%n",
                assessment.getDistance()
        );

        System.out.printf(
                "TTC: %.2fs%n",
                assessment.getTimeToCollision()
        );

        assertEquals(
                ThreatLevel.BRAKE,
                assessment.getThreatLevel()
        );

        System.out.println("\n--- BRAKING ---");

        BrakeDecision command =
                brakeController.execute(assessment);

        assertEquals(
                BrakeResult.FAILED,
                command.getResult()
        );

        double relativeSpeed =
                carState.getCarSpeed() - pedestrianSpeed;

        simulateTick(relativeSpeed);

        BrakeDecision verified =
                brakeController.execute(assessment);

        assertEquals(
                BrakeResult.SUCCESS,
                verified.getResult()
        );

        double expectedDecel = 8.0;

        double actualDecel =
                (INITIAL_SPEED - carState.getCarSpeed())
                        / (TICK_MS / 1000.0);

        System.out.println(
                "\n--- DECELERATION VERIFICATION ---");

        System.out.printf(
                "Expected decel: %.2f m/s²%n",
                expectedDecel
        );

        System.out.printf(
                "Actual decel: %.4f m/s²%n",
                actualDecel
        );

        assertTrue(
                actualDecel >= expectedDecel * 0.95
                        && actualDecel <= expectedDecel * 1.05
        );

        System.out.println(
                "\n--- BRAKING TO STOP ---");

        boolean collisionOccurred = false;

        while (carState.getCarSpeed() > 0) {

            relativeSpeed =
                    carState.getCarSpeed() - pedestrianSpeed;

            simulateTick(relativeSpeed);

            System.out.printf(
                    "t=%4dms | speed=%6.3f m/s (%5.1f km/h)"
                            + " | distance=%7.3fm%n",
                    carState.getCurrentTimeMs(),
                    carState.getCarSpeed(),
                    carState.getCarSpeed() * 3.6,
                    pedestrianDistance
            );

            if (pedestrianDistance <= 0.0) {

                collisionOccurred = true;

                System.out.printf(
                        "*** COLLISION OCCURRED at %.3f m/s ***%n",
                        carState.getCarSpeed()
                );

                break;
            }
        }

        System.out.println("\n--- FINAL RESULT ---");

        if (collisionOccurred) {

            System.out.println(
                    "FAILURE: Car hit pedestrian."
            );

        } else {

            System.out.println(
                    "SUCCESS: Car stopped safely."
            );

            System.out.printf(
                    "Remaining distance: %.3fm%n",
                    pedestrianDistance
            );
        }

        System.out.printf(
                "Final speed: %.4f m/s%n",
                carState.getCarSpeed()
        );

        assertFalse(
                collisionOccurred,
                "Car should stop before impact"
        );

        assertEquals(
                0.0,
                carState.getCarSpeed(),
                0.001
        );
    }
}