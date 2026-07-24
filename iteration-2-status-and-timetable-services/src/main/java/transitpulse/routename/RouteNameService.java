package transitpulse.routename;

import transitpulse.model.RouteEntry;
import transitpulse.model.TransportMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Iteration 1: Route-name Service.
 *
 * Cleans raw route/rank data (bus routes, train routes, minibus taxi ranks)
 * and exposes lookup methods for later services to validate against.
 */
public class RouteNameService {

    // Keyed by RouteEntry.identityKey() -> cleaned entry. LinkedHashMap keeps insertion order stable.
    private final Map<String, RouteEntry> cleanedEntries = new LinkedHashMap<>();

    /**
     * Loads and cleans raw "region | name | code | mode" rows.
     * Cleaning rules:
     *  - trim whitespace on every field
     *  - normalize region/name to Title Case, code to UPPERCASE
     *  - parse mode leniently (accepts "minibus", "minibus taxi", etc. as TAXI)
     *  - de-duplicate rows that refer to the same route/rank + mode, even if
     *    formatting differs (case, spacing)
     */
    public void loadAndClean(List<String> rawRows) {
        for (String row : rawRows) {
            RouteEntry cleaned = cleanRow(row);
            if (cleaned == null) {
                continue; // malformed row, skip rather than crash the whole load
            }
            // First occurrence wins; later duplicates are ignored.
            cleanedEntries.putIfAbsent(cleaned.identityKey() + "|" + cleaned.getMode(), cleaned);
        }
    }

    private RouteEntry cleanRow(String row) {
        if (row == null) return null;
        String[] parts = row.split("\\|");
        if (parts.length != 4) {
            return null; // malformed: skip
        }
        String region = titleCase(parts[0].trim());
        String name = titleCase(parts[1].trim());
        String code = parts[2].trim().toUpperCase();
        TransportMode mode;
        try {
            mode = TransportMode.fromRaw(parts[3]);
        } catch (IllegalArgumentException e) {
            return null; // unrecognized mode: skip rather than crash
        }
        if (region.isEmpty() || name.isEmpty() || code.isEmpty()) {
            return null;
        }
        return new RouteEntry(region, name, code, mode);
    }

    private String titleCase(String s) {
        if (s.isEmpty()) return s;
        String[] words = s.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    /** All cleaned entries, in load order. */
    public List<RouteEntry> all() {
        return Collections.unmodifiableList(new ArrayList<>(cleanedEntries.values()));
    }

    /** Entries for a given region (case-insensitive). */
    public List<RouteEntry> findByRegion(String region) {
        List<RouteEntry> results = new ArrayList<>();
        for (RouteEntry e : cleanedEntries.values()) {
            if (e.getRegion().equalsIgnoreCase(region)) {
                results.add(e);
            }
        }
        return results;
    }

    /**
     * Looks up a single route/rank by region + name (case-insensitive).
     * Used by Status/Timetable services to validate incoming queries.
     */
    public RouteEntry find(String region, String name) {
        for (RouteEntry e : cleanedEntries.values()) {
            if (e.getRegion().equalsIgnoreCase(region) && e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    public boolean exists(String region, String name) {
        return find(region, name) != null;
    }
}
