package com.team30.core.datalayer.observers;

public interface SensorSubject {
    void addObserver(SensorObserver observer);
    void removeObserver(SensorObserver observer);
    void notifyObservers();
}
