package com.team30;

import com.team30.core.datalayer.enums.*;
import com.team30.core.datalayer.sensors.*;
import com.team30.core.logic.*;
import com.team30.core.presentation.DriverInterface;
import com.team30.simulation.engine.ScenarioLoader;
import com.team30.simulation.engine.SimulatorEngine;
import com.team30.simulation.scenario.Scenario;
import com.team30.simulation.state.CarState;
import java.util.Scanner;

import java.util.List;

public class App {

    public static void main(String[] args) {

        // ===== 1. LOAD SCENARIO =====
        Scenario scenario = ScenarioLoader.of().load();
        if (scenario == null) {
            System.out.println("[APP] No scenario selected — exiting.");
            return;
        }
        System.out.println("[APP] Loaded scenario: " + scenario.getScenarioName());

        // ===== 2. CAR STATE =====
        double[] initialRPM = new double[]{500, 500, 500, 500};
        CarState carState = new CarState(
                scenario.getInitialCarSpeed(),
                scenario.getInitialCarSpeed(),
                initialRPM,
                DrivingMode.CRUISING,
                0.0,
                2.0,
                scenario.getInitialWeather(),
                scenario.getInitialLight()
        );

        // ===== 3. AEBS COMPONENTS =====
        DriverInterface       driverInterface   = new DriverInterface(carState);
        SensorInputHandler    inputHandler      = new SensorInputHandler();
        RedundancyChecker     redundancyChecker = new RedundancyChecker();
        CollisionDetector     collisionDetector = new CollisionDetector();
        BrakeSystemController brakeController   = new BrakeSystemController(carState);
        FaultHandler          faultHandler      = new FaultHandler(driverInterface, carState);

        AEBSSoftwareSystem aebs = new AEBSSoftwareSystem(
                inputHandler,
                redundancyChecker,
                collisionDetector,
                brakeController,
                faultHandler,
                driverInterface       // ← added
        );

        // ===== 5. SENSORS =====
        RadarSensor      primaryRadar    = new RadarSensor(SensorId.PRIMARY,   carState);
        RadarSensor      redundantRadar  = new RadarSensor(SensorId.REDUNDANT, carState);
        LidarSensor      primaryLidar    = new LidarSensor(SensorId.PRIMARY,   carState);
        LidarSensor      redundantLidar  = new LidarSensor(SensorId.REDUNDANT, carState);
        CameraSensor     primaryCamera   = new CameraSensor(SensorId.PRIMARY,   carState);
        CameraSensor     redundantCamera = new CameraSensor(SensorId.REDUNDANT, carState);
        WheelSpeedSensor primaryWheel    = new WheelSpeedSensor(SensorId.PRIMARY,   carState);
        WheelSpeedSensor redundantWheel  = new WheelSpeedSensor(SensorId.REDUNDANT, carState);

        // Attach AEBS as observer to all sensors
        List<Sensor> allSensors = List.of(
                primaryRadar,    redundantRadar,
                primaryLidar,    redundantLidar,
                primaryCamera,   redundantCamera,
                primaryWheel,    redundantWheel
        );
        allSensors.forEach(sensor -> sensor.attachObserver(aebs));

        // ===== 6. SIMULATOR ENGINE ====
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n[APP] AEBS is currently ACTIVE by default.");
        System.out.print("[APP] Do you want to deactivate AEBS before starting? (y/n): ");

        String input = scanner.nextLine().trim().toLowerCase();

        if (input.equals("y")) {
            driverInterface.toggleAEBS(false);
        } else {
            driverInterface.toggleAEBS(true);
        }
        SimulatorEngine engine = new SimulatorEngine(carState, scenario, allSensors, aebs, driverInterface.isAesbActive());

        // ===== 7. RUN =====
        System.out.println("[APP] Starting simulation...");

        engine.run();
        System.out.println("[APP] Simulation complete.");
    }
}