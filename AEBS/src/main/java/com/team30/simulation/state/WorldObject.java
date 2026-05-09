package com.team30.simulation.state;

import com.team30.core.datalayer.enums.MovementDirection;
import com.team30.core.datalayer.enums.ObjectType;

public class WorldObject {
    double position;        // metres ahead of car
    double speed;           // km/h
    double lateralPosition; // metres sideways (for crossing objects)
    ObjectType type;
    MovementDirection direction;
    boolean inCurrentLane;

    public WorldObject(double position, double speed, double lateralPosition,
                       ObjectType type, MovementDirection direction, boolean inCurrentLane) {
        this.position = position;
        this.speed = speed;
        this.lateralPosition = lateralPosition;
        this.type = type;
        this.direction = direction;
        this.inCurrentLane = inCurrentLane;
    }

    public double getPosition()        { return position; }
    public double getSpeed()           { return speed; }
    public double getLateralPosition() { return lateralPosition; }
    public ObjectType getType()        { return type; }
    public MovementDirection getDirection() { return direction; }
    public boolean isInCurrentLane()   { return inCurrentLane; }

    public void setPosition(double position)               { this.position = position; }
    public void setSpeed(double speed)                     { this.speed = speed; }
    public void setLateralPosition(double lateralPosition) { this.lateralPosition = lateralPosition; }
    public void setType(ObjectType type)                   { this.type = type; }
    public void setDirection(MovementDirection direction)  { this.direction = direction; }
    public void setInCurrentLane(boolean inCurrentLane)    { this.inCurrentLane = inCurrentLane; }
}