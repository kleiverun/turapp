package com.ole.turapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fetches terrain elevations from the OpenTopoData public API (SRTM 30 m).
 *
 * Rate limit: 1 request / second, 100 locations / request.
 * Self-host at https://www.opentopodata.org/ for better throughput.
 */
@Service
public class ElevationService {

    private static final String URL_BASE =
            "https://api.opentopodata.org/v1/eudem25m?locations=";
    private static final int BATCH = 100;
    private static final long SLEEP_MS = 1_100;

    private final RestTemplate http = new RestTemplate();

    /**
     * Returns one elevation (metres above sea level) per (lats[i], lons[i]) pair.
     * Falls back to 0.0 for points outside SRTM coverage (ocean, polar regions).
     */
    public double[] fetchElevations(double[] lats, double[] lons) {
        int n = lats.length;
        double[] elevs = new double[n];

        for (int start = 0; start < n; start += BATCH) {
            if (start > 0) {
                try { Thread.sleep(SLEEP_MS); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return elevs; // return what we have so far
                }
            }

            int end = Math.min(start + BATCH, n);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) sb.append('|');
                sb.append(lats[i]).append(',').append(lons[i]);
            }

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = http.getForObject(URL_BASE + sb, Map.class);
                if (body == null) continue;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                if (results == null) continue;

                for (int i = 0; i < results.size(); i++) {
                    Object elev = results.get(i).get("elevation");
                    if (elev instanceof Number) elevs[start + i] = ((Number) elev).doubleValue();
                }
            } catch (Exception ignored) {
                // leave zeros for this batch (flat-terrain fallback)
            }
        }
        return elevs;
    }
}
