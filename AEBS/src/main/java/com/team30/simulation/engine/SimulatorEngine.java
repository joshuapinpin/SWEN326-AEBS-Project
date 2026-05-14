package com.team30.simulation.engine;

import com.team30.core.datalayer.data.BrakeDecision;
import com.team30.core.datalayer.enums.*;
import com.team30.core.datalayer.observers.TimeObserver;
import com.team30.core.datalayer.observers.TimeSubject;
import com.team30.core.datalayer.sensors.Sensor;
import com.team30.core.logic.AEBSSoftwareSystem;
import com.team30.simulation.scenario.HazardEvent;
import com.team30.simulation.scenario.Scenario;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * SimulatorEngine is the core of the AEBS simulation. It maintains the current state of the car and the world,
 * processes the scenario's hazard events, updates the physics, and interacts with the AEBS software system.
 */
public class SimulatorEngine implements TimeSubject {

    private static final Logger logger = LogManager.getLogger(SimulatorEngine.class);

    private static final double LANE_WIDTH             = 3.5;
    private static final long   TICK_DURATION_MS       = 10;
    private static final double TICK_DURATION_S        = TICK_DURATION_MS / 1000.0;
    // private static final double MIN_SPEED_MS           = 0.01; unused variable
    private static final double WHEEL_CIRCUMFERENCE    = 2.0;
    private static final double LOCKUP_DECEL_THRESHOLD = 7.85;

    private final CarState           carState;
    private final Scenario           scenario;
    private final List<Sensor>       allSensors;
    private final AEBSSoftwareSystem aebs;
    private final List<TimeObserver> timeObservers = new ArrayList<>();
    private long currentTimeMs;
    private boolean deactivated;

    public SimulatorEngine(CarState carState, Scenario scenario,
                           List<Sensor> allSensors, AEBSSoftwareSystem aebs, boolean deactivated) {
        this.carState      = carState;
        this.scenario      = scenario;
        this.allSensors    = allSensors != null ? allSensors : new ArrayList<>();
        this.aebs          = aebs;
        this.currentTimeMs = 0;
        this.deactivated = deactivated;

        List<WorldObject> initial = scenario.getInitialObjects();
        if (initial != null && !initial.isEmpty()) {
            carState.setObjectsInWorld(new ArrayList<>(initial));
        }
    }

    // -----------------------------------------------------------------------
    // Main loop
    // -----------------------------------------------------------------------

    public void run() {
        System.out.printf("[SIM] '%s' starting — duration=%dms tick=%dms%n",
                scenario.getScenarioName(), scenario.getDurationMs(), TICK_DURATION_MS);

        while (currentTimeMs <= scenario.getDurationMs()) {
            carState.setCurrentTimeMs(currentTimeMs);

            notifyObservers();   // notify time observers
            applyHazardEvents(); // mutate CarState / spawn objects
            fireSensors();       // sensors push into SensorInputHandler via observers
            if(isDeactivated()){
                aebs.runPipeline();  // pull from buffer → assess → brake → fault
            }
            if (aebs.hasCriticalFailure()) {
                System.out.println("[SIM] Critical AEBS failure detected. Ending simulation.");
                break;
            }
            updatePhysics();     // update speed, positions, RPM
            logTickSummary();
            currentTimeMs += TICK_DURATION_MS;

            if (currentTimeMs > 0
                    && scenario.getHazardEvents().stream().allMatch(HazardEvent::isTriggered)
                    && carState.getObjectsInWorld().isEmpty()) {
                break;
            }
        }

    }

    /**
     * If the scenario is marked as deactivated, the AEBS will not run its pipeline
     * and thus will not react to any hazards. This allows testing of the physics and
     * hazard event system in isolation, or simulating a failure mode where the AEBS is offline.
     */
    private boolean isDeactivated() {
        return deactivated;
    }

    // -----------------------------------------------------------------------
    // Hazard events
    // -----------------------------------------------------------------------

    private void applyHazardEvents() {
        List<HazardEvent> hazards = scenario.getHazardEvents();
        if (hazards == null) return;

        for (HazardEvent event : hazards) {
            if (!event.isTriggered() && event.getTriggerTime() <= currentTimeMs) {
                applyHazard(event);
                event.setTriggered(true);
            }
        }
    }

    private void applyHazard(HazardEvent event) {
        switch (event.getType()) {
            case OBJECT_ENTERS_ROAD -> {
                WorldObject obj = new WorldObject(
                        event.getWorldPosition(),
                        event.getObjectSpeed(),
                        0.0,
                        event.getObjectType(),
                        event.getMovementDirection(),
                        event.isInCurrentLane()
                );
                carState.getObjectsInWorld().add(obj);
            }
            case SENSOR_FAILURE -> {
                applySensorFailure(event.getSensorType(), event.getSensorId());
            }
            case WEATHER_CHANGE -> {
                carState.setWeather(event.getNewWeather());
            }
            case LIGHT_CHANGE -> {
                carState.setLight(event.getNewLight());
            }
            case BRAKE_FAILURE -> {
                aebs.setBrakeFailures(event.getFailureCount());
            }
        }
    }

    private void applySensorFailure(SensorType type, SensorId id) {
        boolean isPrimary = id == SensorId.PRIMARY;
        switch (type) {
            case RADAR       -> { if (isPrimary) carState.setPrimaryRadarFailed(true);
            else           carState.setRedundantRadarFailed(true); }
            case LIDAR       -> { if (isPrimary) carState.setPrimaryLidarFailed(true);
            else           carState.setRedundantLidarFailed(true); }
            case CAMERA      -> { if (isPrimary) carState.setPrimaryCameraFailed(true);
            else           carState.setRedundantCameraFailed(true); }
            case WHEEL_SPEED -> { if (isPrimary) carState.setPrimaryWheelFailed(true);
            else           carState.setRedundantWheelFailed(true); }
        }
        for (Sensor sensor : allSensors) {
            if (sensor.getSensorId() == id && sensor.getSensorType() == type) {
                sensor.setWorking(false);
            }
        }
        aebs.showMaintenanceWarning(type);
    }

    // -----------------------------------------------------------------------
    // Physics
    // -----------------------------------------------------------------------

    private void updatePhysics() {
        double speed = carState.getCarSpeed();
        double previousSpeed = speed;
        double decelApplied = 0.0;

        switch (carState.getDrivingMode()) {
            case CRUISING -> {
                double delta = carState.getAccelerationRate() * TICK_DURATION_S;
                speed = Math.min(speed + delta, carState.getTargetSpeed());
            }
            case BRAKING -> {
                double rate  = getDecelerationRate(carState.getWeather());
                decelApplied = rate;
                carState.setDecelerationRate(rate);
                speed = Math.max(speed - rate * TICK_DURATION_S, 0.0);
            }
            case RESUMING -> {
                carState.setDecelerationRate(0.0);
                double delta = carState.getAccelerationRate() / 2.0 * TICK_DURATION_S;
                speed = Math.min(speed + delta, carState.getTargetSpeed());
            }
            case FAIL_SAFE -> {
                double rate  = getDecelerationRate(carState.getWeather());
                decelApplied = rate;
                carState.setDecelerationRate(rate);
                speed = Math.max(speed - rate * TICK_DURATION_S, 0.0);
            }
        }

        double actualDecel = (previousSpeed - speed) / TICK_DURATION_S;

        carState.setActualDeceleration(actualDecel);
        carState.setCarSpeed(speed);

        updateObjectPositions();
        updateWheelRPM(decelApplied);
    }

    private void updateObjectPositions() {
        List<WorldObject> objects = carState.getObjectsInWorld();
        if (objects == null || objects.isEmpty()) return;

        double carSpeed = carState.getCarSpeed();
        List<WorldObject> toRemove = new ArrayList<>();

        for (WorldObject obj : objects) {
            if (obj.getDirection() == MovementDirection.CROSSING) {
                double newLateral = obj.getLateralPosition()
                        + obj.getSpeed() * TICK_DURATION_S;
                obj.setLateralPosition(newLateral);

                if (!obj.isInCurrentLane() && newLateral >= LANE_WIDTH / 2.0) {
                    obj.setInCurrentLane(true);
                }
                if (obj.isInCurrentLane() && newLateral > LANE_WIDTH) {
                    obj.setInCurrentLane(false);
                }
            } else {
                double relativeSpeed = switch (obj.getDirection()) {
                    case SAME_DIRECTION -> carSpeed - obj.getSpeed();
                    case STATIONARY     -> carSpeed;
                    default             -> carSpeed;
                };

                double newPosition = obj.getPosition() - relativeSpeed * TICK_DURATION_S;
                obj.setPosition(newPosition);

                if (newPosition <= 0.0) {
                    toRemove.add(obj);
                }
            }
        }
        objects.removeAll(toRemove);
    }

    private void updateWheelRPM(double decelApplied) {
        double speed     = carState.getCarSpeed();
        double normalRPM = speed * 60.0 / WHEEL_CIRCUMFERENCE;
        boolean rearLockup = decelApplied >= LOCKUP_DECEL_THRESHOLD;


        carState.setWheelRPM(new double[]{
                normalRPM,
                normalRPM,
                rearLockup ? 0.0 : normalRPM,
                rearLockup ? 0.0 : normalRPM
        });
    }

    public double getDecelerationRate(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR      -> 7.85;
            case CLOUDY     -> 7.50;
            case RAIN       -> 6.00;
            case HEAVY_RAIN -> 5.00;
            case FOG        -> 6.50;
            case SNOW       -> 4.00;
            case HEAVY_SNOW -> 2.50;
        };
    }

    // -----------------------------------------------------------------------
    // Sensor firing
    // -----------------------------------------------------------------------

    private void fireSensors() {
        for (Sensor sensor : allSensors) {
            sensor.onTick(currentTimeMs, carState);
        }
    }

    // -----------------------------------------------------------------------
    // TimeSubject
    // -----------------------------------------------------------------------

    @Override
    public void attachObserver(TimeObserver observer) {
        if (observer != null && !timeObservers.contains(observer)) {
            timeObservers.add(observer);
        }
    }

    @Override
    public void deattachObserver(TimeObserver observer) {
        timeObservers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (TimeObserver observer : timeObservers) {
            System.out.println("ADGAWJDHAWBDKAWBDKJAW");
            observer.onTick(currentTimeMs);
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    private void logTickSummary() {
        StringBuilder sb = new StringBuilder();

        BrakeDecision decision = aebs.getLatestBrakeDecision();

        String result = "null";
        int attemptsMade = 0;

        if (decision != null) {
            result = decision.getResult().toString();
            attemptsMade = decision.getAttemptsMade();
        }

        sb.append(String.format(
                "t=%5dms | speed=%5.2fm/s | mode=%-10s | threat=%-10s | result=%-12s | attempts=%d",
                currentTimeMs,
                carState.getCarSpeed(),
                carState.getDrivingMode(),
                aebs.getPreviousThreat(),
                result,
                attemptsMade
        ));

        List<WorldObject> objects = carState.getObjectsInWorld();
        if (objects != null && !objects.isEmpty()) {
            for (WorldObject obj : objects) {
                if (obj.isInCurrentLane()) {
                    sb.append(String.format(" | %s dist=%5.1fm",
                            obj.getType(), obj.getPosition()));
                }
            }
        }

        logger.info(sb.toString());
    }

    public long getCurrentTimeMs() { return currentTimeMs; }
    public CarState getCarState() { return carState; }
    public Scenario getScenario() { return scenario; }
    public List<Sensor> getAllSensors() { return allSensors; }

}