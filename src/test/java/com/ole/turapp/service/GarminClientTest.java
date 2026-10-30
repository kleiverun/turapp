package com.ole.turapp.service;

import com.ole.turapp.exception.GarminUnavailableException;
import com.ole.turapp.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GarminClientTest {

    private static final String BASE = "https://garmin.test";
    private static final String TOKEN_URL = BASE + GarminClient.TOKEN_PATH;
    private static final String DOWNLOAD_URL = BASE + "/download-service/files/activity/42";
    private static final byte[] ZIP = {'P', 'K', 3, 4};

    private MockRestServiceServer server;
    private MutableClock clock;
    private GarminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        clock = new MutableClock(Instant.parse("2026-10-28T10:00:00Z"));
        client = new GarminClient(builder, "secret-key", clock);
    }

    private void expectToken(String token) {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "ApiKey secret-key"))
                .andRespond(withSuccess(token, MediaType.TEXT_PLAIN));
    }

    private void expectDownload(String token) {
        server.expect(requestTo(DOWNLOAD_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess(ZIP, MediaType.APPLICATION_OCTET_STREAM));
    }

    @Test
    void downloadActivityFile_FetchesTokenThenFile() {
        expectToken("jwt-1");
        expectDownload("jwt-1");

        byte[] file = client.downloadActivityFile(42);

        assertThat(file).isEqualTo(ZIP);
        server.verify();
    }

    @Test
    void downloadActivityFile_ReusesTokenUntilItExpires() {
        expectToken("jwt-1");
        expectDownload("jwt-1");
        expectDownload("jwt-1");
        expectToken("jwt-2");
        expectDownload("jwt-2");

        client.downloadActivityFile(42);
        clock.advance(Duration.ofHours(22));
        client.downloadActivityFile(42);
        clock.advance(Duration.ofHours(2));
        client.downloadActivityFile(42);

        server.verify();
    }

    @Test
    void downloadActivityFile_TokenRejected_RenewsTokenAndRetriesOnce() {
        expectToken("jwt-1");
        server.expect(requestTo(DOWNLOAD_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectToken("jwt-2");
        expectDownload("jwt-2");

        assertThat(client.downloadActivityFile(42)).isEqualTo(ZIP);
        server.verify();
    }

    @Test
    void downloadActivityFile_UnknownActivity_ThrowsNotFound() {
        expectToken("jwt-1");
        server.expect(requestTo(DOWNLOAD_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.downloadActivityFile(42))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find Garmin activity 42");
    }

    @Test
    void downloadActivityFile_GarminDown_ThrowsGarminUnavailable() {
        expectToken("jwt-1");
        server.expect(requestTo(DOWNLOAD_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.downloadActivityFile(42))
                .isInstanceOf(GarminUnavailableException.class);
    }

    @Test
    void downloadActivityFile_InvalidApiKey_ThrowsGarminUnavailable() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.downloadActivityFile(42))
                .isInstanceOf(GarminUnavailableException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void downloadActivityFile_NoApiKey_ThrowsWithoutCallingGarmin() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer noCalls = MockRestServiceServer.bindTo(builder).build();
        GarminClient unconfigured = new GarminClient(builder, "  ", clock);

        assertThatThrownBy(() -> unconfigured.downloadActivityFile(42))
                .isInstanceOf(GarminUnavailableException.class)
                .hasMessageContaining("not configured");
        noCalls.verify();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
