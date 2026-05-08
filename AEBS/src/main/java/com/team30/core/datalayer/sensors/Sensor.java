package com.team30.core.datalayer.sensors;

import com.team30.core.datalayer.observers.SensorObserver;
import com.team30.core.datalayer.observers.SensorSubject;

import java.util.List;

public abstract class Sensor implements SensorSubject {
    boolean isSensorFaulty = false;
    List<SensorObserver> observers;

    public void addObserver(SensorObserver observer){
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }
    public void removeObserver(SensorObserver observer){
        observers.remove(observer);
    }

    public void notifyObservers(){
        for (SensorObserver observer : observers) {
            // Todo: need to find out how to get  data and pass it.
            // observer.update(data);
        }
    }
}
