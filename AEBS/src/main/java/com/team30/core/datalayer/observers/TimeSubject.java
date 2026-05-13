package com.team30.core.datalayer.observers;

/**
 * Subject interface for managing time observers.
 * The simulation environment will implement this interface to allow the AEBSSoftwareSystem to
 * register as an observer and receive time updates.
 */
public interface TimeSubject {

    /**
     * Registers an observer to receive time updates.
     * @param observer The time observer to register.
     */
    void attachObserver(TimeObserver observer);

    /**
     * Removes an observer from receiving time updates.
     * @param observer The time observer to remove.
     */
    void deattachObserver(TimeObserver observer);

    /**
     * Notifies all registered observers of a time tick.
     * This method will be called by the simulation environment on each time tick,
     * and it will trigger the onTick method in all registered observers (like the SimulationEngine)
     * to perform time-based processing.
     */
    void notifyObservers();
}
