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

    private CarState carState;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeController;
    private double pedestrianDistance; // tracked across ticks


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

        RadarData  radar  = new RadarData(SensorId.PRIMARY, ts, distance, relativeSpeed, true);
        CameraData camera = new CameraData(SensorId.PRIMARY, ts, ObjectType.PEDESTRIAN, inLane, 0.9);

        Map<SensorId, SensorData> radarMap  = new HashMap<>();
        Map<SensorId, SensorData> cameraMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY,  radar);
        cameraMap.put(SensorId.PRIMARY, camera);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();
        readings.put(SensorType.RADAR,  radarMap);
        readings.put(SensorType.CAMERA, cameraMap);

        return new ProcessedSensorData(readings, ts);
    }

    private void simulateTick(double relativeSpeed) {
        carState.setCurrentTimeMs(carState.getCurrentTimeMs() + TICK_MS);

        double decel = carState.getDecelerationRate();
        if (decel > 0) {
            double newSpeed = Math.max(0.0,
                    carState.getCarSpeed() - decel * (TICK_MS / 1000.0));
            carState.setCarSpeed(newSpeed);

            double newRPM = (newSpeed / WHEEL_CIRCUMFERENCE) * 60.0;
            if (decel >= 8.0) {
                carState.setWheelRPM(new double[]{newRPM, newRPM, 0.0, 0.0});
            } else {
                carState.setWheelRPM(new double[]{newRPM, newRPM, newRPM, newRPM});
            }
        }

        // Update distance — relative speed closes the gap each tick
        // relativeSpeed = carSpeed - pedestrianSpeed
        // use current carSpeed for this tick's closing
        double closingThisTick = relativeSpeed * (TICK_MS / 1000.0);
        pedestrianDistance = Math.max(0.0, pedestrianDistance - closingThisTick);
    }

    @Test
    void fullScenario_pedestrianDetected_carBrakesToStop() {
        pedestrianDistance = 30.0;
        double pedestrianSpeed = 0.0; // stationary pedestrian

        System.out.println("=== PEDESTRIAN BRAKING SCENARIO ===");
        System.out.printf("Initial speed:        %.2f m/s (%.1f km/h)%n",
                carState.getCarSpeed(), carState.getCarSpeed() * 3.6);
        System.out.printf("Pedestrian distance:  %.2fm%n", pedestrianDistance);
        System.out.printf("Pedestrian speed:     %.2f m/s (stationary)%n", pedestrianSpeed);

        ProcessedSensorData data = buildPedestrianDetected(pedestrianDistance, 16.67, true);

        RadarData  radar  = (RadarData)  data.getSensorData(SensorType.RADAR,  SensorId.PRIMARY);
        CameraData camera = (CameraData) data.getSensorData(SensorType.CAMERA, SensorId.PRIMARY);

        System.out.println("\n--- SENSOR READINGS ---");
        System.out.printf("RADAR  | distance=%.2fm | relativeSpeed=%.2fm/s | detected=%b%n",
                radar.getDistance(), radar.getRelativeSpeed(), radar.isObjectDetected());
        System.out.printf("CAMERA | objectType=%s | inLane=%b | confidence=%.2f%n",
                camera.getClassification(), camera.isInCurrentLane(), camera.getConfidence());
        System.out.println("NOTE: ObjectType.PEDESTRIAN manually injected — in real simulation");
        System.out.println("      this comes from WorldObject.getType() set by SimulatorEngine.");

        CollisionAssessment assessment = collisionDetector.assess(data);

        System.out.println("\n--- COLLISION ASSESSMENT ---");
        System.out.printf("ThreatLevel:   %s%n",   assessment.getThreatLevel());
        System.out.printf("TTC:           %.2fs%n", assessment.getTimeToCollision());
        System.out.printf("Distance:      %.2fm%n", assessment.getDistance());
        System.out.printf("ObjectType:    %s%n",    assessment.getObjectType());
        System.out.printf("ObjectInLane:  %b%n",    assessment.isObjectInLane());
        System.out.println("Pedestrian brake threshold: 2.5s — TTC 1.80s is below → BRAKE");

        assertEquals(ThreatLevel.BRAKE, assessment.getThreatLevel());

        // Tick 0: command
        System.out.println("\n--- BRAKING ---");
        BrakeDecision command = brakeController.execute(assessment);
        System.out.printf("t=%4dms | COMMAND | carSpeed=%6.4f m/s | distance=%.4fm%n",
                0, carState.getCarSpeed(), pedestrianDistance);
        assertEquals(BrakeResult.FAILED, command.getResult());

        // Tick 1: verify
        double relativeSpeed = carState.getCarSpeed() - pedestrianSpeed;
        simulateTick(relativeSpeed);
        BrakeDecision verified = brakeController.execute(assessment);

        double expectedDecel = 8.0;
        double actualDecel   = (INITIAL_SPEED - carState.getCarSpeed()) / (TICK_MS / 1000.0);

        System.out.printf("t=%4dms | VERIFY  | carSpeed=%6.4f m/s | distance=%.4fm%n",
                10, carState.getCarSpeed(), pedestrianDistance);
        System.out.println("\n--- DECELERATION VERIFICATION (±5%) ---");
        System.out.printf("Expected decel:  %.2f m/s²%n", expectedDecel);
        System.out.printf("Actual decel:    %.4f m/s²%n", actualDecel);
        System.out.printf("Lower bound:     %.2f m/s²%n", expectedDecel * 0.95);
        System.out.printf("Upper bound:     %.2f m/s²%n", expectedDecel * 1.05);
        System.out.printf("Within ±5%%:     %b%n",
                actualDecel >= expectedDecel * 0.95 && actualDecel <= expectedDecel * 1.05);

        assertEquals(BrakeResult.SUCCESS, verified.getResult());

        // Run to stop
        System.out.println("\n--- BRAKING TO STOP ---");
        int tickCount = 1;
        boolean collisionOccurred = false;

        while (carState.getCarSpeed() > 0) {
            relativeSpeed = carState.getCarSpeed() - pedestrianSpeed;
            simulateTick(relativeSpeed);
            tickCount++;

            System.out.printf("t=%4dms | carSpeed=%6.4f m/s (%5.1f km/h) | distance=%6.4fm%n",
                    tickCount * 10,
                    carState.getCarSpeed(),
                    carState.getCarSpeed() * 3.6,
                    pedestrianDistance);

            if (pedestrianDistance <= 0.0) {
                collisionOccurred = true;
                System.out.printf("*** COLLISION at t=%dms! carSpeed=%.4f m/s ***%n",
                        tickCount * 10, carState.getCarSpeed());
                break;
            }
        }

        System.out.println("\n--- RESULT ---");
        if (collisionOccurred) {
            System.out.printf("COLLISION OCCURRED at t=%dms — car did not stop in time!%n",
                    tickCount * 10);
        } else {
            System.out.printf("Car stopped safely at t=%dms (%.2fs)%n",
                    tickCount * 10, tickCount * 10 / 1000.0);
            System.out.printf("Remaining distance to pedestrian: %.4fm%n", pedestrianDistance);
        }
        System.out.printf("Final speed: %.4f m/s%n", carState.getCarSpeed());

        assertFalse(collisionOccurred, "Car should stop before hitting pedestrian");
        assertEquals(0.0, carState.getCarSpeed(), 0.001);
    }
}