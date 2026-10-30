package com.ole.turapp.service;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Garmin FIT activity file into a trip summary and its GPS points.
 *
 * <p>Points come from the {@code record} messages. Records without a position
 * (typically logged before the watch had a GPS fix) or without a timestamp
 * are skipped. FIT stores
 * coordinates as semicircles, which are converted to WGS84 decimal degrees.
 *
 * <p>Start time, duration, distance and sport come from the first {@code session}
 * message. If the file has no session (e.g. a truncated recording), they are
 * derived from the points instead.
 */
@Component
public class FitActivityParser {

    /** Degrees per semicircle: 180 / 2^31. */
    static final double SEMICIRCLES_TO_DEGREES = 180.0 / 2147483648.0;

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /** One activity parsed from FIT: summary fields plus the ordered GPS points. */
    public record ParsedActivity(
            String sport,
            Instant startedAt,
            Instant endedAt,
            long durationSeconds,
            double distanceMeters,
            List<ParsedPoint> points) {}

    /** A GPS point in WGS84 decimal degrees, altitude in meters (may be {@code null}). */
    public record ParsedPoint(double latitude, double longitude, Double altitude, Instant timestamp) {}

    /**
     * Reads the activity from the given FIT stream.
     *
     * @throws IllegalArgumentException if the stream is not a readable FIT activity
     */
    public ParsedActivity parse(InputStream fit) {
        List<ParsedPoint> points = new ArrayList<>();
        List<SessionMesg> sessions = new ArrayList<>();

        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        broadcaster.addListener((RecordMesg record) -> {
            ParsedPoint point = toPoint(record);
            if (point != null) {
                points.add(point);
            }
        });
        broadcaster.addListener((SessionMesg session) -> sessions.add(session));

        try {
            if (!decode.read(fit, broadcaster, broadcaster)) {
                throw new IllegalArgumentException("Could not read the FIT file: incomplete data");
            }
        } catch (FitRuntimeException e) {
            throw new IllegalArgumentException("Could not read the FIT file: " + e.getMessage(), e);
        }

        if (sessions.isEmpty() && points.isEmpty()) {
            throw new IllegalArgumentException("The FIT file contains no activity data");
        }

        return sessions.isEmpty()
                ? fromPoints(points)
                : fromSession(sessions.get(0), points);
    }

    private ParsedPoint toPoint(RecordMesg record) {
        Integer lat = record.getPositionLat();
        Integer lon = record.getPositionLong();
        if (lat == null || lon == null || record.getTimestamp() == null) {
            return null;
        }
        Float altitude = record.getEnhancedAltitude() != null
                ? record.getEnhancedAltitude()
                : record.getAltitude();
        return new ParsedPoint(
                lat * SEMICIRCLES_TO_DEGREES,
                lon * SEMICIRCLES_TO_DEGREES,
                altitude == null ? null : altitude.doubleValue(),
                toInstant(record.getTimestamp()));
    }

    private ParsedActivity fromSession(SessionMesg session, List<ParsedPoint> points) {
        Instant startedAt = toInstant(session.getStartTime());
        if (startedAt == null && !points.isEmpty()) {
            startedAt = points.get(0).timestamp();
        }

        Float elapsed = session.getTotalElapsedTime();
        long durationSeconds;
        Instant endedAt;
        if (elapsed != null && startedAt != null) {
            durationSeconds = Math.round(elapsed);
            endedAt = startedAt.plusSeconds(durationSeconds);
        } else {
            endedAt = lastTimestamp(points);
            durationSeconds = secondsBetween(startedAt, endedAt);
        }

        Float distance = session.getTotalDistance();
        double distanceMeters = distance != null ? distance : pathLength(points);

        String sport = session.getSport() != null ? session.getSport().name() : null;
        return new ParsedActivity(sport, startedAt, endedAt, durationSeconds, distanceMeters, points);
    }

    private ParsedActivity fromPoints(List<ParsedPoint> points) {
        Instant startedAt = points.get(0).timestamp();
        Instant endedAt = lastTimestamp(points);
        return new ParsedActivity(
                null, startedAt, endedAt, secondsBetween(startedAt, endedAt), pathLength(points), points);
    }

    private Instant lastTimestamp(List<ParsedPoint> points) {
        return points.isEmpty() ? null : points.get(points.size() - 1).timestamp();
    }

    private long secondsBetween(Instant from, Instant to) {
        if (from == null || to == null) {
            return 0;
        }
        return Math.max(0, Duration.between(from, to).getSeconds());
    }

    private double pathLength(List<ParsedPoint> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            ParsedPoint a = points.get(i - 1);
            ParsedPoint b = points.get(i);
            total += haversine(a.latitude(), a.longitude(), b.latitude(), b.longitude());
        }
        return total;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

    private Instant toInstant(DateTime dateTime) {
        return dateTime == null ? null : dateTime.getDate().toInstant();
    }
}
