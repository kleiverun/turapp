package com.ole.turapp.service;

import com.ole.turapp.dto.GarminImportResponse;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.exception.GarminActivityAlreadyImportedException;
import com.ole.turapp.exception.GarminUnavailableException;
import com.ole.turapp.service.FitActivityParser.ParsedActivity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports Garmin Connect activities as trips: download the original file,
 * pull the {@code .fit} out of the ZIP, parse it and save it with its track points.
 *
 * <p>The Personal API key belongs to one Garmin account, so only the Turlogg user
 * configured as its owner ({@code app.garmin.personal-api-user-id}) may import.
 * Multi-user import needs OAuth through Garmin's Starter Access program.
 */
@Service
public class GarminService {

    /** Guards against ZIP bombs; real activity files are a few MB at most. */
    static final int MAX_FIT_BYTES = 50 * 1024 * 1024;

    private static final ZoneId NAME_ZONE = ZoneId.of("Europe/Oslo");

    private final GarminClient garminClient;
    private final FitActivityParser fitParser;
    private final TripService tripService;
    private final Long personalApiUserId;

    public GarminService(GarminClient garminClient, FitActivityParser fitParser, TripService tripService,
                         @Value("${app.garmin.personal-api-user-id:#{null}}") Long personalApiUserId) {
        this.garminClient = garminClient;
        this.fitParser = fitParser;
        this.tripService = tripService;
        this.personalApiUserId = personalApiUserId;
    }

    public GarminImportResponse importActivity(Long userId, long activityId) {
        requirePersonalApiOwner(userId);
        if (tripService.isGarminActivityImported(userId, activityId)) {
            throw new GarminActivityAlreadyImportedException(activityId);
        }

        byte[] download = garminClient.downloadActivityFile(activityId);
        ParsedActivity activity = fitParser.parse(new ByteArrayInputStream(extractFit(download)));
        TripResponse trip = saveTrip(userId, activityId, activity);

        return new GarminImportResponse(activityId, activity.points().size(), trip);
    }

    /** Two simultaneous imports can both pass the check above; the unique constraint stops the second. */
    private TripResponse saveTrip(Long userId, long activityId, ParsedActivity activity) {
        try {
            return tripService.importTrip(userId, tripName(activity), activity, activityId);
        } catch (DataIntegrityViolationException e) {
            if (tripService.isGarminActivityImported(userId, activityId)) {
                throw new GarminActivityAlreadyImportedException(activityId);
            }
            throw e;
        }
    }

    private void requirePersonalApiOwner(Long userId) {
        if (personalApiUserId == null) {
            throw new GarminUnavailableException(
                    "Garmin import is not configured (app.garmin.personal-api-user-id is missing)");
        }
        if (!personalApiUserId.equals(userId)) {
            throw new AccessDeniedException("Garmin import is only available to the owner of the Garmin account");
        }
    }

    /** Returns the {@code .fit} inside a Garmin ZIP, or the bytes as-is if they are not a ZIP. */
    byte[] extractFit(byte[] download) {
        if (!isZip(download)) {
            return download;
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(download))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".fit")) {
                    byte[] fit = zip.readNBytes(MAX_FIT_BYTES + 1);
                    if (fit.length > MAX_FIT_BYTES) {
                        throw new IllegalArgumentException("The FIT file in the Garmin download is too large");
                    }
                    return fit;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not unpack the Garmin download: " + e.getMessage(), e);
        }
        throw new IllegalArgumentException("The Garmin download contains no .fit file (only FIT activities can be imported)");
    }

    private boolean isZip(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    /** E.g. "Cross country skiing 2026-09-19", or "Garmin activity" when nothing is known. */
    String tripName(ParsedActivity activity) {
        String sport = activity.sport() == null || activity.sport().isBlank()
                ? "Garmin activity"
                : capitalize(activity.sport().replace('_', ' ').toLowerCase(Locale.ROOT));
        if (activity.startedAt() == null) {
            return sport;
        }
        return sport + " " + activity.startedAt().atZone(NAME_ZONE).toLocalDate();
    }

    private String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
