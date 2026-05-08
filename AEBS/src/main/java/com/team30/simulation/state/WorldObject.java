package com.team30.simulation.state;

import com.team30.core.datalayer.enums.MovementDirection;
import com.team30.core.datalayer.enums.ObjectType;

public class WorldObject {

    // Distance ahead of the car in metres
    private double position;

    // Speed of the object in km/h
    private double speed;

    // Sideways offset from lane centre in metres
    private double lateralPosition;

    private ObjectType type;

    private MovementDirection direction;

    private boolean inCurrentLane;

    public WorldObject(double position, double speed, double lateralPosition, ObjectType type, MovementDirection direction, boolean inCurrentLane) {
        this.position = position;
        this.speed = speed;
        this.lateralPosition = lateralPosition;
        this.type = type;
        this.direction = direction;
        this.inCurrentLane = inCurrentLane;
    }

    // --- Getters ---

    public double getPosition() {
        return position;
    }

    public double getSpeed() {
        return speed;
    }

    public double getLateralPosition() {
        return lateralPosition;
    }

    public ObjectType getType() {
        return type;
    }

    public MovementDirection getDirection() {
        return direction;
    }

    public boolean isInCurrentLane() {
        return inCurrentLane;
    }

    // --- Setters ---

    public void setPosition(double position) {
        this.position = position;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setLateralPosition(double lateralPosition) {
        this.lateralPosition = lateralPosition;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public void setDirection(MovementDirection direction) {
        this.direction = direction;
    }

    public void setInCurrentLane(boolean inCurrentLane) {
        this.inCurrentLane = inCurrentLane;
    }

    @Override
    public String toString() {
        return "WorldObject{" +
                "position=" + position +
                ", speed=" + speed +
                ", lateralPosition=" + lateralPosition +
                ", type=" + type +
                ", direction=" + direction +
                ", inCurrentLane=" + inCurrentLane +
                '}';
    }
}
