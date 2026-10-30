package com.ole.turapp.exception;

/** Garmin Connect could not be reached, rejected our credentials, or import is not configured. */
public class GarminUnavailableException extends RuntimeException {
    public GarminUnavailableException(String message) {
        super(message);
    }
}
