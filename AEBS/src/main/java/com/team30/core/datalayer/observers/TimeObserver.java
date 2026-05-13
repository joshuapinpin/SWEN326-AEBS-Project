package com.team30.core.datalayer.observers;

/**
 * Observer interface for receiving time updates.
 * This can be used by the simulation environment to notify the AEBSSoftwareSystem of time ticks,
 * allowing it to perform time-based processing.
 */
public interface TimeObserver {

    /**
     * Called on each time tick to allow the AEBSSoftwareSystem to perform time-based processing.
     * @param currentTimeMs The current simulation time in milliseconds.
     */
    void onTick(long currentTimeMs);
}
