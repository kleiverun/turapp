package com.ole.turapp.service;

import com.garmin.fit.BufferEncoder;
import com.garmin.fit.DateTime;
import com.garmin.fit.File;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.ole.turapp.service.FitActivityParser.ParsedActivity;
import com.ole.turapp.service.FitActivityParser.ParsedPoint;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class FitActivityParserTest {

    private static final Instant START = Instant.parse("2026-10-20T08:00:00Z");

    private final FitActivityParser parser = new FitActivityParser();

    @Test
    void parse_WithSession_UsesSessionSummaryAndConvertsSemicircles() {
        List<Mesg> mesgs = new ArrayList<>();
        mesgs.add(record(START, 59.9, 10.7, 100f, null));
        mesgs.add(record(START.plusSeconds(10), 59.91, 10.71, 105f, null));
        mesgs.add(session(START, 3600f, 5000f, Sport.HIKING));

        ParsedActivity activity = parser.parse(fit(mesgs));

        assertThat(activity.sport()).isEqualTo("HIKING");
        assertThat(activity.startedAt()).isEqualTo(START);
        assertThat(activity.endedAt()).isEqualTo(START.plusSeconds(3600));
        assertThat(activity.durationSeconds()).isEqualTo(3600);
        assertThat(activity.distanceMeters()).isEqualTo(5000.0);
        assertThat(activity.points()).hasSize(2);
        ParsedPoint first = activity.points().get(0);
        assertThat(first.latitude()).isCloseTo(59.9, within(1e-6));
        assertThat(first.longitude()).isCloseTo(10.7, within(1e-6));
        assertThat(first.altitude()).isCloseTo(100.0, within(0.5));
        assertThat(first.timestamp()).isEqualTo(START);
    }

    @Test
    void parse_RecordsWithoutPosition_AreSkipped() {
        RecordMesg noFix = new RecordMesg();
        noFix.setTimestamp(new DateTime(java.util.Date.from(START)));
        noFix.setHeartRate((short) 90);

        List<Mesg> mesgs = new ArrayList<>();
        mesgs.add(noFix);
        mesgs.add(record(START.plusSeconds(5), 59.9, 10.7, null, null));
        mesgs.add(session(START, 60f, 10f, Sport.WALKING));

        ParsedActivity activity = parser.parse(fit(mesgs));

        assertThat(activity.points()).hasSize(1);
        assertThat(activity.points().get(0).timestamp()).isEqualTo(START.plusSeconds(5));
        assertThat(activity.points().get(0).altitude()).isNull();
    }

    @Test
    void parse_EnhancedAltitude_IsPreferredOverAltitude() {
        List<Mesg> mesgs = List.of(
                record(START, 59.9, 10.7, 100f, 1234f),
                session(START, 60f, 0f, Sport.HIKING));

        ParsedActivity activity = parser.parse(fit(mesgs));

        assertThat(activity.points().get(0).altitude()).isCloseTo(1234.0, within(0.5));
    }

    @Test
    void parse_WithoutSession_DerivesSummaryFromPoints() {
        // 0.01 degrees of latitude is about 1112 meters.
        List<Mesg> mesgs = List.of(
                record(START, 59.90, 10.7, null, null),
                record(START.plusSeconds(600), 59.91, 10.7, null, null));

        ParsedActivity activity = parser.parse(fit(mesgs));

        assertThat(activity.sport()).isNull();
        assertThat(activity.startedAt()).isEqualTo(START);
        assertThat(activity.endedAt()).isEqualTo(START.plusSeconds(600));
        assertThat(activity.durationSeconds()).isEqualTo(600);
        assertThat(activity.distanceMeters()).isCloseTo(1112.0, within(5.0));
    }

    @Test
    void parse_NotAFitFile_ThrowsIllegalArgument() {
        InputStream garbage = new ByteArrayInputStream("this is not a fit file at all".getBytes());

        assertThatThrownBy(() -> parser.parse(garbage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FIT");
    }

    @Test
    void parse_NoActivityData_ThrowsIllegalArgument() {
        assertThatThrownBy(() -> parser.parse(fit(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no activity data");
    }

    private InputStream fit(List<Mesg> mesgs) {
        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(File.ACTIVITY);
        fileId.setManufacturer(Manufacturer.DEVELOPMENT);
        fileId.setTimeCreated(new DateTime(java.util.Date.from(START)));

        BufferEncoder encoder = new BufferEncoder();
        encoder.write(fileId);
        encoder.write(mesgs);
        return new ByteArrayInputStream(encoder.close());
    }

    private RecordMesg record(Instant time, double lat, double lon, Float altitude, Float enhancedAltitude) {
        RecordMesg record = new RecordMesg();
        record.setTimestamp(new DateTime(java.util.Date.from(time)));
        record.setPositionLat(toSemicircles(lat));
        record.setPositionLong(toSemicircles(lon));
        if (altitude != null) {
            record.setAltitude(altitude);
        }
        if (enhancedAltitude != null) {
            record.setEnhancedAltitude(enhancedAltitude);
        }
        return record;
    }

    private SessionMesg session(Instant start, float elapsedSeconds, float distanceMeters, Sport sport) {
        SessionMesg session = new SessionMesg();
        session.setTimestamp(new DateTime(java.util.Date.from(start.plusSeconds((long) elapsedSeconds))));
        session.setStartTime(new DateTime(java.util.Date.from(start)));
        session.setTotalElapsedTime(elapsedSeconds);
        session.setTotalDistance(distanceMeters);
        session.setSport(sport);
        return session;
    }

    private int toSemicircles(double degrees) {
        return (int) Math.round(degrees / FitActivityParser.SEMICIRCLES_TO_DEGREES);
    }
}
