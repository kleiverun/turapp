package com.ole.turapp.service;

import com.ole.turapp.dto.GarminImportResponse;
import com.ole.turapp.dto.TripResponse;
import com.ole.turapp.exception.GarminActivityAlreadyImportedException;
import com.ole.turapp.exception.GarminUnavailableException;
import com.ole.turapp.service.FitActivityParser.ParsedActivity;
import com.ole.turapp.service.FitActivityParser.ParsedPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GarminServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final long ACTIVITY_ID = 24431089654L;
    private static final byte[] FIT_BYTES = {14, 16, 0x5A, 0x0D, '.', 'F', 'I', 'T'};

    @Mock
    private GarminClient garminClient;

    @Mock
    private FitActivityParser fitParser;

    @Mock
    private TripService tripService;

    private GarminService garminService;

    @BeforeEach
    void setUp() {
        garminService = new GarminService(garminClient, fitParser, tripService, OWNER_ID);
    }

    private ParsedActivity activity(String sport, Instant startedAt) {
        List<ParsedPoint> points = List.of(
                new ParsedPoint(60.0, 9.3, 650.0, startedAt),
                new ParsedPoint(60.1, 9.4, 700.0, startedAt.plusSeconds(60)));
        return new ParsedActivity(sport, startedAt, startedAt.plusSeconds(3600), 3600, 2700.0, points);
    }

    private byte[] zip(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    void importActivity_ZipWithFit_ParsesFitAndSavesTrip() throws IOException {
        Instant start = Instant.parse("2026-09-19T16:09:38Z");
        ParsedActivity parsed = activity("HIKING", start);
        TripResponse trip = new TripResponse(7L, "Hiking 2026-09-19", null, 2700.0, 3600L, "PRIVATE",
                start, start.plusSeconds(3600));
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(zip("24431089654_ACTIVITY.fit", FIT_BYTES));
        when(fitParser.parse(any())).thenReturn(parsed);
        when(tripService.importTrip(OWNER_ID, "Hiking 2026-09-19", parsed, ACTIVITY_ID)).thenReturn(trip);

        GarminImportResponse response = garminService.importActivity(OWNER_ID, ACTIVITY_ID);

        assertThat(response.garminActivityId()).isEqualTo(ACTIVITY_ID);
        assertThat(response.pointCount()).isEqualTo(2);
        assertThat(response.trip()).isEqualTo(trip);
        ArgumentCaptor<InputStream> fitStream = ArgumentCaptor.forClass(InputStream.class);
        verify(fitParser).parse(fitStream.capture());
        assertThat(fitStream.getValue().readAllBytes()).isEqualTo(FIT_BYTES);
    }

    @Test
    void importActivity_RawFitDownload_IsParsedAsIs() throws IOException {
        Instant start = Instant.parse("2026-09-19T16:09:38Z");
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(FIT_BYTES);
        when(fitParser.parse(any())).thenReturn(activity("HIKING", start));

        garminService.importActivity(OWNER_ID, ACTIVITY_ID);

        ArgumentCaptor<InputStream> fitStream = ArgumentCaptor.forClass(InputStream.class);
        verify(fitParser).parse(fitStream.capture());
        assertThat(fitStream.getValue().readAllBytes()).isEqualTo(FIT_BYTES);
    }

    @Test
    void importActivity_ZipWithoutFit_ThrowsIllegalArgument() throws IOException {
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(zip("activity.gpx", "<gpx/>".getBytes()));

        assertThatThrownBy(() -> garminService.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no .fit file");

        verifyNoInteractions(fitParser);
        verify(tripService, never()).importTrip(any(), any(), any(), any());
    }

    @Test
    void importActivity_AlreadyImported_ThrowsConflictWithoutDownloading() {
        when(tripService.isGarminActivityImported(OWNER_ID, ACTIVITY_ID)).thenReturn(true);

        assertThatThrownBy(() -> garminService.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(GarminActivityAlreadyImportedException.class)
                .hasMessageContaining(String.valueOf(ACTIVITY_ID));

        verifyNoInteractions(garminClient, fitParser);
        verify(tripService, never()).importTrip(any(), any(), any(), any());
    }

    @Test
    void importActivity_LostRaceToSimultaneousImport_ThrowsConflict() {
        when(tripService.isGarminActivityImported(OWNER_ID, ACTIVITY_ID)).thenReturn(false, true);
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(FIT_BYTES);
        when(fitParser.parse(any())).thenReturn(activity("HIKING", Instant.parse("2026-09-19T16:09:38Z")));
        when(tripService.importTrip(any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_trips_user_garmin_activity"));

        assertThatThrownBy(() -> garminService.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(GarminActivityAlreadyImportedException.class);
    }

    @Test
    void importActivity_OtherIntegrityViolation_IsRethrown() {
        when(tripService.isGarminActivityImported(OWNER_ID, ACTIVITY_ID)).thenReturn(false);
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(FIT_BYTES);
        when(fitParser.parse(any())).thenReturn(activity("HIKING", Instant.parse("2026-09-19T16:09:38Z")));
        when(tripService.importTrip(any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("something else"));

        assertThatThrownBy(() -> garminService.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void importActivity_NotGarminAccountOwner_ThrowsAccessDenied() {
        assertThatThrownBy(() -> garminService.importActivity(2L, ACTIVITY_ID))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(garminClient, fitParser, tripService);
    }

    @Test
    void importActivity_OwnerNotConfigured_ThrowsGarminUnavailable() {
        GarminService unconfigured = new GarminService(garminClient, fitParser, tripService, null);

        assertThatThrownBy(() -> unconfigured.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(GarminUnavailableException.class)
                .hasMessageContaining("not configured");

        verify(garminClient, never()).downloadActivityFile(anyLong());
    }

    @Test
    void importActivity_UnparseableFit_DoesNotSaveTrip() {
        when(garminClient.downloadActivityFile(ACTIVITY_ID)).thenReturn(FIT_BYTES);
        when(fitParser.parse(any())).thenThrow(new IllegalArgumentException("Could not read the FIT file"));

        assertThatThrownBy(() -> garminService.importActivity(OWNER_ID, ACTIVITY_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verify(tripService, never()).importTrip(any(), any(), any(), any());
    }

    @Test
    void tripName_SportWithUnderscores_IsReadable() {
        // 23:30 UTC is already the next day in Norway.
        ParsedActivity parsed = activity("CROSS_COUNTRY_SKIING", Instant.parse("2026-01-10T23:30:00Z"));

        assertThat(garminService.tripName(parsed)).isEqualTo("Cross country skiing 2026-01-11");
    }

    @Test
    void tripName_NoSportOrStart_FallsBackToGenericName() {
        ParsedActivity parsed = new ParsedActivity(null, null, null, 0, 0, List.of());

        assertThat(garminService.tripName(parsed)).isEqualTo("Garmin activity");
    }
}
