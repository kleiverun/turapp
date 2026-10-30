package com.ole.turapp.service;

import com.ole.turapp.exception.GarminUnavailableException;
import com.ole.turapp.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Talks to the Garmin Connect Personal API (pull-only, read-only, one Garmin account).
 *
 * <p>The API key is exchanged for a bearer token that lives 24 hours. The token is
 * cached and renewed a little before it expires, or right away if Garmin rejects it.
 */
@Component
public class GarminClient {

    static final String TOKEN_PATH = "/personalapitoken-service/personal-api/personal-api-key/token";
    static final String DOWNLOAD_PATH = "/download-service/files/activity/{activityId}";

    /** Garmin tokens last 24 h; renew an hour early so a token never expires mid-request. */
    static final Duration TOKEN_LIFETIME = Duration.ofHours(23);

    private final RestClient http;
    private final String apiKey;
    private final Clock clock;

    private String token;
    private Instant tokenExpiresAt = Instant.MIN;

    @Autowired
    public GarminClient(
            @Value("${app.garmin.base-url:https://personal.connectapi.garmin.com}") String baseUrl,
            @Value("${app.garmin.api-key:}") String apiKey) {
        this(RestClient.builder().baseUrl(baseUrl), apiKey, Clock.systemUTC());
    }

    GarminClient(RestClient.Builder builder, String apiKey, Clock clock) {
        this.http = builder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.clock = clock;
    }

    /**
     * Downloads the original file for an activity: normally a ZIP containing one {@code .fit}.
     *
     * @throws NotFoundException if Garmin has no activity with this id
     * @throws GarminUnavailableException if Garmin cannot be reached or rejects our credentials
     */
    public byte[] downloadActivityFile(long activityId) {
        try {
            return download(activityId);
        } catch (HttpClientErrorException.Unauthorized e) {
            // The cached token was revoked or expired early: get a fresh one and try once more.
            invalidateToken();
            try {
                return download(activityId);
            } catch (HttpClientErrorException.Unauthorized retry) {
                throw new GarminUnavailableException("Garmin rejected the access token");
            }
        }
    }

    private byte[] download(long activityId) {
        try {
            byte[] body = http.get()
                    .uri(DOWNLOAD_PATH, activityId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                throw new GarminUnavailableException("Garmin returned an empty file for activity " + activityId);
            }
            return body;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Did not find Garmin activity " + activityId);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw e;
        } catch (RestClientException e) {
            throw new GarminUnavailableException("Could not download the activity from Garmin");
        }
    }

    private synchronized String token() {
        if (apiKey.isEmpty()) {
            throw new GarminUnavailableException("Garmin import is not configured (app.garmin.api-key is missing)");
        }
        if (token != null && clock.instant().isBefore(tokenExpiresAt)) {
            return token;
        }
        String body;
        try {
            body = http.post()
                    .uri(TOKEN_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKey)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new GarminUnavailableException("Could not get an access token from Garmin");
        }
        // The body is the raw JWT; strip whitespace and quotes in case it comes JSON-encoded.
        String fresh = body == null ? "" : body.trim().replaceAll("^\"|\"$", "");
        if (fresh.isEmpty()) {
            throw new GarminUnavailableException("Garmin returned an empty access token");
        }
        token = fresh;
        tokenExpiresAt = clock.instant().plus(TOKEN_LIFETIME);
        return token;
    }

    private synchronized void invalidateToken() {
        token = null;
    }
}
