package com.ole.turapp.service;

import com.ole.turapp.dto.NatureReserveBoundaryPoint;
import com.ole.turapp.dto.NatureReserveResponse;
import com.ole.turapp.model.NatureReserve;
import com.ole.turapp.model.NatureReservePoint;
import com.ole.turapp.repository.NatureReserveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class NatureReserveService {

    private static final Logger log = LoggerFactory.getLogger(NatureReserveService.class);
    private static final int MAX_POINTS = 60;
    private static final int BATCH_SIZE = 2000;
    private static final String BASE_URL =
            "https://kart.miljodirektoratet.no/arcgis/rest/services/vern/MapServer/0/query"
            + "?where=verneform%3D%27Naturreservat%27"
            + "&outFields=navn&f=json&outSR=4326&returnGeometry=true"
            + "&resultRecordCount=" + BATCH_SIZE;

    private final NatureReserveRepository reserveRepository;
    private final RestTemplate http = new RestTemplate();

    public NatureReserveService(NatureReserveRepository reserveRepository) {
        this.reserveRepository = reserveRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        if (reserveRepository.count() > 0) return;
        log.info("Fetching nature reserves from Miljødirektoratet...");
        log.info("Import result: {}", importNow());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String importNow() {
        try {
            reserveRepository.deleteAll();
            int offset = 0, total = 0;
            boolean more = true;

            while (more) {
                String url = BASE_URL + "&resultOffset=" + offset;
                Map<String, Object> body = http.getForObject(URI.create(url), Map.class);
                if (body == null || body.containsKey("error"))
                    return "ERROR from ArcGIS at offset " + offset + ": " + (body != null ? body.get("error") : "null");

                List<Map<String, Object>> features = (List<Map<String, Object>>) body.get("features");
                if (features == null || features.isEmpty()) break;

                for (Map<String, Object> feature : features) {
                    Map<String, Object> attrs = (Map<String, Object>) feature.get("attributes");
                    Map<String, Object> geom  = (Map<String, Object>) feature.get("geometry");
                    if (attrs == null || geom == null) continue;

                    String name = (String) attrs.get("navn");
                    if (name == null || name.isBlank()) continue;

                    List<double[]> ring = NationalParkService.outerRing(geom);
                    if (ring.isEmpty()) continue;

                    NatureReserve reserve = new NatureReserve(name);
                    List<double[]> pts = NationalParkService.decimate(ring, MAX_POINTS);
                    for (int i = 0; i < pts.size(); i++) {
                        reserve.getPoints().add(new NatureReservePoint(reserve, i, pts.get(i)[1], pts.get(i)[0]));
                    }
                    reserveRepository.save(reserve);
                    total++;
                }

                Boolean exceeded = (Boolean) body.get("exceededTransferLimit");
                more = Boolean.TRUE.equals(exceeded);
                offset += features.size();
                log.info("Loaded batch at offset {}, total so far: {}", offset, total);
            }
            return "OK: " + total + " nature reserves loaded";
        } catch (Exception e) {
            log.error("Nature reserve import failed", e);
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    public List<NatureReserveResponse> getAll() {
        return reserveRepository.findAll().stream()
                .map(r -> new NatureReserveResponse(r.getId(), r.getName(),
                        r.getPoints().stream()
                                .map(pt -> new NatureReserveBoundaryPoint(pt.getLatitude(), pt.getLongitude()))
                                .toList()))
                .toList();
    }
}
