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

public class PedestrianClearedTest {

    private static final double WHEEL_CIRCUMFERENCE = 2.0;
    private static final long   TICK_MS             = 10;
    private static final double INITIAL_SPEED       = 16.67;

    private CarState carState;
    private CollisionDetector collisionDetector;
    private BrakeSystemController brakeController;

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

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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

    private ProcessedSensorData buildNoThreat() {
        long ts = carState.getCurrentTimeMs();
        RadarData radar = new RadarData(SensorId.PRIMARY, ts, -1.0, 0.0, false);

        Map<SensorId, SensorData> radarMap = new HashMap<>();
        radarMap.put(SensorId.PRIMARY, radar);

        Map<SensorType, Map<SensorId, SensorData>> readings = new HashMap<>();
        readings.put(SensorType.RADAR, radarMap);
        return new ProcessedSensorData(readings, ts);
    }

    private void simulateTick() {
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
        } else if (carState.getDrivingMode() == DrivingMode.RESUMING) {
            // Accelerate back toward target speed
            double newSpeed = Math.min(
                    carState.getTargetSpeed(),
                    carState.getCarSpeed() + carState.getAccelerationRate() * (TICK_MS / 1000.0));
            carState.setCarSpeed(newSpeed);
            double newRPM = (newSpeed / WHEEL_CIRCUMFERENCE) * 60.0;
            carState.setWheelRPM(new double[]{newRPM, newRPM, newRPM, newRPM});
        }
    }

    // -----------------------------------------------------------------------
    // Test
    // -----------------------------------------------------------------------

    @Test
    void pedestrianDetected_brakes_clears_resumes() {
        System.out.println("=== PEDESTRIAN DETECTED, BRAKES, CLEARS, RESUMES ===");
        System.out.printf("Initial speed: %.2f m/s (%.1f km/h)%n",
                carState.getCarSpeed(), carState.getCarSpeed() * 3.6);

        // ===== PHASE 1: DETECT =====
        System.out.println("\n--- PHASE 1: PEDESTRIAN DETECTED ---");
        ProcessedSensorData threat = buildPedestrianDetected(30.0, 16.67, true);
        CollisionAssessment threatAssessment = collisionDetector.assess(threat);

        System.out.printf("ThreatLevel:  %s%n",   threatAssessment.getThreatLevel());
        System.out.printf("TTC:          %.2fs%n", threatAssessment.getTimeToCollision());
        System.out.printf("ObjectType:   %s%n",    threatAssessment.getObjectType());
        System.out.printf("ObjectInLane: %b%n",    threatAssessment.isObjectInLane());
        assertEquals(ThreatLevel.BRAKE, threatAssessment.getThreatLevel());

        // ===== PHASE 2: BRAKE =====
        System.out.println("\n--- PHASE 2: BRAKING ---");

        // Tick 0: command
        BrakeDecision command = brakeController.execute(threatAssessment);
        System.out.printf("t=%4dms | COMMAND | speed=%6.4f m/s | mode=%s%n",
                0, carState.getCarSpeed(), carState.getDrivingMode());
        assertEquals(BrakeResult.FAILED,  command.getResult());
        assertEquals(DrivingMode.BRAKING, carState.getDrivingMode());

        // Tick 1: verify
        simulateTick();
        BrakeDecision verified = brakeController.execute(threatAssessment);
        System.out.printf("t=%4dms | VERIFY  | speed=%6.4f m/s | result=%s%n",
                10, carState.getCarSpeed(), verified.getResult());
        assertEquals(BrakeResult.SUCCESS, verified.getResult());

        // Brake for 500ms (50 ticks) — simulate partial braking before pedestrian clears
        double speedWhenCleared = 0;
        for (int i = 2; i <= 50; i++) {
            simulateTick();
            System.out.printf("t=%4dms | BRAKING | speed=%6.4f m/s (%5.1f km/h)%n",
                    i * 10,
                    carState.getCarSpeed(),
                    carState.getCarSpeed() * 3.6);
        }
        speedWhenCleared = carState.getCarSpeed();

        // ===== PHASE 3: PEDESTRIAN CLEARS =====
        System.out.println("\n--- PHASE 3: PEDESTRIAN CLEARED ---");
        simulateTick();
        ProcessedSensorData cleared = buildNoThreat();
        CollisionAssessment clearedAssessment = collisionDetector.assess(cleared);

        System.out.printf("ThreatLevel:  %s — no object detected%n",
                clearedAssessment.getThreatLevel());

        BrakeDecision clearDecision = brakeController.execute(clearedAssessment);
        System.out.printf("t=%4dms | CLEARED | speed=%6.4f m/s | result=%s | mode=%s%n",
                510, carState.getCarSpeed(), clearDecision.getResult(), carState.getDrivingMode());

        assertEquals(BrakeResult.CLEARED,    clearDecision.getResult());
        assertEquals(DrivingMode.RESUMING,   carState.getDrivingMode());
        assertEquals(0.0, carState.getDecelerationRate(), 0.001);

        // ===== PHASE 4: RESUME =====
        System.out.println("\n--- PHASE 4: RESUMING TO TARGET SPEED ---");
        System.out.printf("Speed when cleared: %.4f m/s (%.1f km/h)%n",
                speedWhenCleared, speedWhenCleared * 3.6);
        System.out.printf("Target speed:       %.2f m/s (%.1f km/h)%n",
                carState.getTargetSpeed(), carState.getTargetSpeed() * 3.6);

        int tickCount = 51;
        while (carState.getCarSpeed() < carState.getTargetSpeed()) {
            simulateTick();
            tickCount++;
            System.out.printf("t=%4dms | RESUMING | speed=%6.4f m/s (%5.1f km/h)%n",
                    tickCount * 10,
                    carState.getCarSpeed(),
                    carState.getCarSpeed() * 3.6);
        }

        System.out.println("\n--- RESULT ---");
        System.out.printf("Resumed to target speed at t=%dms%n", tickCount * 10);
        System.out.printf("Final speed: %.4f m/s (%.1f km/h)%n",
                carState.getCarSpeed(), carState.getCarSpeed() * 3.6);

        assertEquals(INITIAL_SPEED, carState.getCarSpeed(), 0.01,
                "Car should resume to original target speed");
        assertEquals(DrivingMode.RESUMING, carState.getDrivingMode());
    }
}