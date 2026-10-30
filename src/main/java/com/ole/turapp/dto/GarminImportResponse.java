package com.ole.turapp.dto;

/**
 * Result of importing one Garmin activity: the Garmin id it came from,
 * how many GPS points were saved, and the created trip.
 */
public record GarminImportResponse(
        long garminActivityId,
        int pointCount,
        TripResponse trip
) {}
