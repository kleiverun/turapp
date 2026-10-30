package com.ole.turapp.exception;

/** The user has already imported this Garmin activity as a trip. */
public class GarminActivityAlreadyImportedException extends RuntimeException {
    public GarminActivityAlreadyImportedException(long activityId) {
        super("Garmin activity " + activityId + " has already been imported");
    }
}
