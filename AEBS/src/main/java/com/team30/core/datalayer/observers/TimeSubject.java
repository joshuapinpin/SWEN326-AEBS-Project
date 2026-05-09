package com.team30.core.datalayer.observers;

public interface TimeSubject {
    void addObserver(TimeObserver observer);
    void removeObserver(TimeObserver observer);
    void notifyObservers();
}
