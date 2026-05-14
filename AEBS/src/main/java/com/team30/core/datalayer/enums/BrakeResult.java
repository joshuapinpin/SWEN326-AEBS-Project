package com.team30.core.datalayer.enums;

/**
 *
 * Enum representing the possible results of a braking action in the AEBS system.
 * Defines outcomes such as SUCCESS, FAILED, EXHAUSTED, CLEARED, and NOT_NEEDED to indicate the status of brake interventions.
 * Ensure the sensor alert only does it once
 */
public enum BrakeResult {
    SUCCESS,
    FAILED,
    EXHAUSTED,
    NOT_NEEDED
}