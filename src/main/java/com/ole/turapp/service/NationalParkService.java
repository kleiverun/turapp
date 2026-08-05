package com.ole.turapp.service;

import com.ole.turapp.dto.NationalParkBoundaryPoint;
import com.ole.turapp.dto.NationalParkResponse;
import com.ole.turapp.model.NationalPark;
import com.ole.turapp.model.NationalParkPoint;
import com.ole.turapp.repository.NationalParkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NationalParkService {

    private static final Logger log = LoggerFactory.getLogger(NationalParkService.class);
    private static final int MAX_POINTS = 300;

    private final NationalParkRepository parkRepository;
    private final RestTemplate http = new RestTemplate();

    public NationalParkService(NationalParkRepository parkRepository) {
        this.parkRepository = parkRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        if (parkRepository.count() > 0) return;
        log.info("Fetching national parks from Miljødirektoratet...");
        log.info("Import result: {}", importNow());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String importNow() {
        try {
            parkRepository.deleteAll();
            String url = "https://kart.miljodirektoratet.no/arcgis/rest/services/vern/MapServer/0/query"
                    + "?where=verneform%3D%27Nasjonalpark%27"
                    + "&outFields=navn&f=json&outSR=4326&returnGeometry=true";

            Map<String, Object> body = http.getForObject(URI.create(url), Map.class);
            if (body == null) return "ERROR: null response";
            if (body.containsKey("error")) return "ERROR from ArcGIS: " + body.get("error");

            List<Map<String, Object>> features = (List<Map<String, Object>>) body.get("features");
            if (features == null || features.isEmpty()) return "ERROR: no features. Keys: " + body.keySet();

            int saved = 0, skipped = 0;
            for (Map<String, Object> feature : features) {
                Map<String, Object> attrs = (Map<String, Object>) feature.get("attributes");
                Map<String, Object> geom  = (Map<String, Object>) feature.get("geometry");
                if (attrs == null || geom == null) { skipped++; continue; }

                String name = (String) attrs.get("navn");
                if (name == null || name.isBlank()) { skipped++; continue; }

                List<double[]> ring = outerRing(geom);
                if (ring.isEmpty()) { skipped++; continue; }

                NationalPark park = new NationalPark(name, null);
                List<double[]> pts = decimate(ring, MAX_POINTS);
                for (int i = 0; i < pts.size(); i++) {
                    park.getPoints().add(new NationalParkPoint(park, i, pts.get(i)[1], pts.get(i)[0]));
                }
                parkRepository.save(park);
                saved++;
            }
            return String.format("OK: %d saved, %d skipped, total=%d", saved, skipped, features.size());
        } catch (Exception e) {
            log.error("National park import failed", e);
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    public List<NationalParkResponse> getAll() {
        return parkRepository.findAll().stream()
                .map(p -> new NationalParkResponse(p.getId(), p.getName(),
                        p.getPoints().stream()
                                .map(pt -> new NationalParkBoundaryPoint(pt.getLatitude(), pt.getLongitude()))
                                .toList()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    static List<double[]> outerRing(Map<String, Object> geom) {
        List<List<List<Double>>> rings = (List<List<List<Double>>>) geom.get("rings");
        if (rings == null || rings.isEmpty()) return List.of();
        List<List<Double>> ring = rings.get(0);
        List<double[]> result = new ArrayList<>(ring.size());
        for (List<Double> pt : ring) {
            result.add(new double[]{pt.get(0), pt.get(1)});
        }
        return result;
    }

    static List<double[]> decimate(List<double[]> ring, int max) {
        if (ring.size() <= max) return ring;
        int stride = (int) Math.ceil((double) ring.size() / max);
        List<double[]> result = new ArrayList<>(max + 1);
        for (int i = 0; i < ring.size(); i += stride) result.add(ring.get(i));
        double[] last = ring.get(ring.size() - 1);
        if (result.get(result.size() - 1) != last) result.add(last);
        return result;
    }
}
