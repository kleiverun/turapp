package com.ole.turapp.service;

import com.ole.turapp.dto.TerrainRouteRequest;
import com.ole.turapp.dto.TerrainRouteResponse;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Finds the lowest-cost off-trail hiking path between two coordinates using
 * a terrain grid and A* weighted by Tobler's hiking function.
 *
 * Grid: ROWS × COLS interior nodes arranged in a corridor between start and end.
 * Elevation: fetched from OpenTopoData (EU-DEM 25 m) in three batches.
 * Cost: distance / Tobler_speed(slope), with steep-terrain multipliers so the
 * algorithm strongly avoids slopes above 26° and treats slopes above 40° as
 * near-impassable (×20 cost).
 */
@Service
public class TerrainRouteService {

    /** Interior grid dimensions. Total elevation queries = ROWS * COLS + 2. */
    private static final int ROWS = 30;
    private static final int COLS = 25;

    /** Corridor half-width as a fraction of total route distance. Capped at 15 km. */
    private static final double CORRIDOR_FRACTION = 1.50;
    private static final double CORRIDOR_MAX_M    = 15_000.0;

    private static final double EARTH_R      = 6_371_000.0;
    private static final double MAX_SPEED_MS = 6.0 * 1_000.0 / 3_600.0; // 6 km/h flat ground

    private final ElevationService elevationService;

    public TerrainRouteService(ElevationService elevationService) {
        this.elevationService = elevationService;
    }

    public TerrainRouteResponse route(TerrainRouteRequest req) {
        double fromLat = req.fromLat(), fromLon = req.fromLon();
        double toLat   = req.toLat(),   toLon   = req.toLon();

        double totalDist = haversine(fromLat, fromLon, toLat, toLon);
        if (totalDist > 100_000)
            throw new IllegalArgumentException("Route too long — maximum 100 km.");
        if (totalDist < 10)
            return straight(fromLat, fromLon, toLat, toLon, 0, 0);

        // ----- Build node coordinate arrays -----
        // Node 0       = start
        // Node 1..N-2  = ROWS × COLS grid interior
        // Node N-1     = end
        int N   = ROWS * COLS + 2;
        int END = N - 1;

        double[] lat  = new double[N];
        double[] lon  = new double[N];
        lat[0] = fromLat; lon[0] = fromLon;
        lat[END] = toLat; lon[END] = toLon;

        // Perpendicular unit vector in metric coordinates
        double midLat  = (fromLat + toLat) / 2.0;
        double lonScale = 111_000.0 * Math.cos(Math.toRadians(midLat));
        double fwdX    = (toLon - fromLon) * lonScale;
        double fwdY    = (toLat - fromLat) * 111_000.0;
        double fwdLen  = Math.sqrt(fwdX * fwdX + fwdY * fwdY);
        double perpX   = -fwdY / fwdLen;  // east component of perpendicular
        double perpY   =  fwdX / fwdLen;  // north component of perpendicular

        double corridorM = Math.min(CORRIDOR_FRACTION * totalDist, CORRIDOR_MAX_M);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = r * COLS + c + 1;
                double t = (r + 1.0) / (ROWS + 1.0);   // 0 < t < 1 along route
                // s ∈ [-1, 1] across the corridor width
                double s = COLS > 1
                        ? (c - (COLS - 1) / 2.0) / ((COLS - 1) / 2.0)
                        : 0.0;

                double baseLat = fromLat + t * (toLat - fromLat);
                double baseLon = fromLon + t * (toLon - fromLon);
                double offsetM = s * corridorM;

                lat[idx] = baseLat + offsetM * perpY / 111_000.0;
                lon[idx] = baseLon + offsetM * perpX / lonScale;
            }
        }

        // ----- Fetch elevations for all nodes -----
        double[] elev = elevationService.fetchElevations(lat, lon);

        // ----- A* -----
        double[] g    = new double[N];
        int[]    prev = new int[N];
        Arrays.fill(g, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        g[0] = 0;

        // PQ entry: [f = g + h, nodeIndex]
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.offer(new double[]{ heuristic(lat[0], lon[0], toLat, toLon), 0 });

        boolean[] visited = new boolean[N];

        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int u = (int) cur[1];
            if (visited[u]) continue;
            visited[u] = true;
            if (u == END) break;

            for (int v : neighbors(u, END)) {
                if (visited[v]) continue;
                double newG = g[u] + edgeCost(lat[u], lon[u], elev[u], lat[v], lon[v], elev[v]);
                if (newG < g[v]) {
                    g[v] = newG;
                    prev[v] = u;
                    pq.offer(new double[]{ newG + heuristic(lat[v], lon[v], toLat, toLon), v });
                }
            }
        }

        // ----- Reconstruct path -----
        List<Integer> path = new ArrayList<>();
        for (int v = END; v != -1; v = prev[v]) path.add(v);
        Collections.reverse(path);

        if (path.isEmpty() || path.get(0) != 0 || path.get(path.size() - 1) != END)
            return straight(fromLat, fromLon, toLat, toLon, elev[0], elev[END]);

        // ----- Build response -----
        List<TerrainRouteResponse.PointDto> pts = new ArrayList<>(path.size());
        double dist = 0, ascent = 0, descent = 0;
        for (int i = 0; i < path.size(); i++) {
            int idx = path.get(i);
            pts.add(new TerrainRouteResponse.PointDto(lat[idx], lon[idx]));
            if (i > 0) {
                int prev_idx = path.get(i - 1);
                dist += haversine(lat[prev_idx], lon[prev_idx], lat[idx], lon[idx]);
                double de = elev[idx] - elev[prev_idx];
                if (de > 0) ascent  += de;
                else         descent -= de;
            }
        }
        return new TerrainRouteResponse(pts, dist, ascent, descent);
    }

    // ------ Graph topology ------

    private List<Integer> neighbors(int u, int END) {
        List<Integer> result = new ArrayList<>(10);
        if (u == 0) {
            // Start → all first-row nodes
            for (int c = 0; c < COLS; c++) result.add(c + 1);
            return result;
        }
        if (u == END) return result;

        int gi = u - 1;                     // 0-based grid index
        int r  = gi / COLS, c = gi % COLS;

        // 8-directional within grid
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS)
                    result.add(nr * COLS + nc + 1);
            }
        }
        // Last row → end
        if (r == ROWS - 1) result.add(END);
        return result;
    }

    // ------ Cost functions ------

    private double edgeCost(double la1, double lo1, double el1,
                             double la2, double lo2, double el2) {
        double d = haversine(la1, lo1, la2, lo2);
        if (d < 0.001) return 0;
        double slope    = (el2 - el1) / d;
        double absSlope = Math.abs(slope);

        // Tobler's hiking function: base cost in seconds
        double speedMps = 6.0 * Math.exp(-3.5 * Math.abs(slope + 0.05)) * 1_000.0 / 3_600.0;
        double cost = d / Math.max(speedMps, 0.01);

        // Slopes above 35° are treated as walls — the A* must route around them.
        // Slopes 26–35° are heavily penalised so the algorithm strongly prefers detours.
        // tan(26°) ≈ 0.49, tan(35°) ≈ 0.70
        if (absSlope > 0.70) return Double.MAX_VALUE / 2;  // impassable: force detour
        if (absSlope > 0.49) return cost * 50;             // very steep: strongly avoid
        if (absSlope > 0.30) return cost * 5;              // ~17°: tiring, prefer flatter
        return cost;
    }

    /** Admissible A* heuristic: straight-line distance / fastest possible speed. */
    private double heuristic(double la, double lo, double toLat, double toLon) {
        return haversine(la, lo, toLat, toLon) / MAX_SPEED_MS;
    }

    private double haversine(double la1, double lo1, double la2, double lo2) {
        double dLat = Math.toRadians(la2 - la1);
        double dLon = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_R * Math.asin(Math.sqrt(a));
    }

    private TerrainRouteResponse straight(double la1, double lo1, double la2, double lo2,
                                           double el1, double el2) {
        double de = el2 - el1;
        return new TerrainRouteResponse(
                List.of(new TerrainRouteResponse.PointDto(la1, lo1),
                        new TerrainRouteResponse.PointDto(la2, lo2)),
                haversine(la1, lo1, la2, lo2),
                Math.max(de, 0),
                Math.max(-de, 0)
        );
    }
}
