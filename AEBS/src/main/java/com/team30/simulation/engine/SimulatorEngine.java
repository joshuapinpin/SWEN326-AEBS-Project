package com.team30.simulation.engine;

import com.team30.core.datalayer.enums.MovementDirection;
import com.team30.core.datalayer.enums.WeatherCondition;
import com.team30.core.datalayer.observers.TimeObserver;
import com.team30.core.datalayer.observers.TimeSubject;
import com.team30.core.datalayer.sensors.Sensor;
import com.team30.simulation.scenario.HazardEvent;
import com.team30.simulation.scenario.Scenario;
import com.team30.simulation.state.CarState;
import com.team30.simulation.state.WorldObject;

import java.util.ArrayList;
import java.util.List;

import static com.team30.core.logic.BrakeSystemController.WHEEL_CIRCUMFERENCE;

public class SimulatorEngine implements TimeSubject {
    private static final double LANE_WIDTH = 3.5; // in meters
    private static final long TICK_DURATION_MS = 10; // 10 ms per tick = 100 ticks per second
    private static final double TICK_DURATION_S = TICK_DURATION_MS / 1000.0;
    private static final double MIN_SPEED_MS = 0.01; // minimum speed in m/s (full stop)
    private static final double LOCKUP_DECEL_THRESHOLD = 7.85; // deceleration rate in m/s²

    // Fields
    private CarState carState;
    private final Scenario scenario;
    private final TimeSubject timeSubject;   // holds `this`
    private final List<Sensor> allSensors;
    private long currentTimeMs;

    private final List<TimeObserver> timeObservers = new ArrayList<>();


    /**
     * Constructs a SimulatorEngine and seeds initial world objects from the
     * scenario into the car state.
     *
     * @param carState   mutable vehicle and world state for this run
     * @param scenario   duration, initial world objects, and scheduled hazards
     * @param allSensors sensors whose {@link Sensor#onTick} fires each tick
     */
    public SimulatorEngine(CarState carState, Scenario scenario, List<Sensor> allSensors) {
        this.carState      = carState;
        this.scenario      = scenario;
        this.allSensors    = allSensors != null ? allSensors : new ArrayList<>();
        this.currentTimeMs = 0;
        this.timeSubject   = this; // engine is its own TimeSubject

        // Copy the scenario's initial objects into the live car state.
        List<WorldObject> initial = null; // Todo: = scenario.getInitialObjects();
        if (initial != null && !initial.isEmpty()) {
            carState.setObjectsInWorld(new ArrayList<>(initial));
        }
    }

    // -----------------------------------------------------------------------
    // Main loop
    // -----------------------------------------------------------------------

    /**
     * Runs the simulation synchronously until the scenario's duration elapses
     * or the car comes to a full stop.
     */
    public void run() {
        System.out.printf(
                "[SIM] '%s' starting: duration %d ms, tick %d ms%n",
                scenario.getScenarioName(), scenario.getDurationMs(), TICK_DURATION_MS
        );

        while(currentTimeMs <= scenario.getDurationMs()){
            carState.setCurrentTimeMs(currentTimeMs);

            notifyObservers(); // notify time observers at the start of the tick
            applyHazardEvents(); // apply any scheduled hazard events for this tick
            updatePhysics(); // update the car's position and speed based on current state
            fireSensors(); // call onTick for each sensor to update their readings

            currentTimeMs += TICK_DURATION_MS; // advance time by one tick

            if (carState.getCarSpeed() < MIN_SPEED_MS) {
                System.out.printf("[SIM] Car stopped at t=%d ms%n", currentTimeMs);
                break;
            }
        }

        System.out.printf(
                "[SIM] Simulation ended at t=%d ms  final speed=%.3f m/s%n",
                currentTimeMs, carState.getCarSpeed()
        );
    }


    private void applyHazardEvents(){
        List<HazardEvent> hazards = scenario.getHazardEvents();
        if (hazards != null) return;

        for(HazardEvent event : hazards){
            if (!event.isTriggered() && event.getTriggerTime() <= currentTimeMs){
                //Todo: event.apply(carState);
                event.setTriggered(true);
                System.out.printf("[SIM] Applied hazard '%s' at t=%d ms%n",
                        event.getType(), currentTimeMs);
            }
        }
    }

    private void updatePhysics(){
        double speed        = carState.getCarSpeed();
        double decelApplied = 0.0;

        switch (carState.getDrivingMode()) {
            case CRUISING -> {
                double delta = carState.getAccelerationRate() * TICK_DURATION_S;
                speed = Math.min(speed + delta, carState.getTargetSpeed());
            }
            case BRAKING -> {
                double rate = getDecelerationRate(carState.getWeather());
                decelApplied = rate;
                carState.setDecelerationRate(rate); // keep CarState consistent
                speed = Math.max(speed - rate * TICK_DURATION_S, 0.0);
            }
            case RESUMING -> {
                double delta = (carState.getAccelerationRate() / 2.0) * TICK_DURATION_S;
                speed = Math.min(speed + delta, carState.getTargetSpeed());
            }
        }

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
                // --- Lateral (crossing) movement ---
                double newLateral = obj.getLateralPosition() + obj.getSpeed() * TICK_DURATION_S;
                obj.setLateralPosition(newLateral);

                // Object enters the ego lane.
                if (!obj.isInCurrentLane() && newLateral >= LANE_WIDTH / 2.0) {
                    obj.setInCurrentLane(true);
                    System.out.printf(
                            "[SIM] t=%5d ms  CROSSING %s entered ego lane (lateral=%.2f m)%n",
                            currentTimeMs, obj.getType(), newLateral);
                }

                // Object exits the ego lane after fully crossing.
                if (obj.isInCurrentLane() && newLateral > LANE_WIDTH) {
                    obj.setInCurrentLane(false);
                    System.out.printf(
                            "[SIM] t=%5d ms  CROSSING %s exited ego lane (lateral=%.2f m)%n",
                            currentTimeMs, obj.getType(), newLateral);
                }

            } else {
                // --- Longitudinal (ahead/behind) movement ---
                double relativeSpeed = switch (obj.getDirection()) {
                    case SAME_DIRECTION -> carSpeed - obj.getSpeed();
                    case STATIONARY     -> carSpeed;
                    default             -> carSpeed; // unreachable; CROSSING handled above
                };

                double newPosition = obj.getPosition() - relativeSpeed * TICK_DURATION_S;
                obj.setPosition(newPosition);

                if (newPosition <= 0.0) {
                    toRemove.add(obj);
                    System.out.printf(
                            "[SIM] t=%5d ms  %s reached car position — removing%n",
                            currentTimeMs, obj.getType());
                }
            }
        }

        objects.removeAll(toRemove);
    }

    private void updateWheelRPM(double decelApplied) {
        double speed     = carState.getCarSpeed();
        double normalRPM = (speed * 60.0) / WHEEL_CIRCUMFERENCE;

        boolean rearLockup = decelApplied >= LOCKUP_DECEL_THRESHOLD;

        if (rearLockup) {
            System.out.printf("[SIM] t=%5d ms  rear-wheel lockup (decel=%.2f m/s²)%n",
                    currentTimeMs, decelApplied);
        }

        carState.setWheelRPM(new double[]{
                normalRPM,                   // front-left
                normalRPM,                   // front-right
                rearLockup ? 0.0 : normalRPM, // rear-left
                rearLockup ? 0.0 : normalRPM  // rear-right
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

    private void fireSensors(){
        for (Sensor sensor : allSensors) {
            sensor.onTick(currentTimeMs, carState);
        }
    }

    /**
     * Registers a {@link TimeObserver} to be notified at the start of each
     * tick. Duplicate registrations are ignored.
     */
    @Override
    public void addObserver(TimeObserver observer) {
        if (observer != null && !timeObservers.contains(observer)) {
            timeObservers.add(observer);
        }
    }

    /** Removes a previously registered {@link TimeObserver}. */
    @Override
    public void removeObserver(TimeObserver observer) {
        timeObservers.remove(observer);
    }

    /**
     * Notifies all registered {@link TimeObserver}s with the current
     * simulated time. Called at the start of every tick.
     */
    @Override
    public void notifyObservers() {
        for (TimeObserver observer : timeObservers) {
            observer.onTick(currentTimeMs);
        }
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /** Returns the current simulated time in milliseconds. */
    public long getCurrentTimeMs()      { return currentTimeMs; }

    /** Returns the mutable car state. */
    public CarState getCarState()       { return carState; }

    /** Returns the scenario being simulated. */
    public Scenario getScenario()       { return scenario; }

    /** Returns all sensors registered with this engine. */
    public List<Sensor> getAllSensors() { return allSensors; }

}
