package transitpulse.timetable;

import com.sun.net.httpserver.HttpServer;
import transitpulse.model.RouteEntry;
import transitpulse.model.TransportMode;
import transitpulse.routename.RouteNameService;
import transitpulse.util.HttpUtil;
import transitpulse.util.SimpleJson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Iteration 2: Timetable Service.
 *
 * Exposes GET /timetable?region=..&name=..
 * Returns a fixed timetable for bus/train, or rank-based departure info
 * for taxis (which depart once loaded rather than on a fixed schedule).
 */
public class TimetableService {

    private final RouteNameService routeNameService;
    private HttpServer server;

    public TimetableService(RouteNameService routeNameService) {
        this.routeNameService = routeNameService;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/timetable", exchange -> {
            try {
                Map<String, String> params = HttpUtil.parseQuery(exchange.getRequestURI());
                String region = params.get("region");
                String name = params.get("name");

                if (region == null || name == null) {
                    HttpUtil.sendJson(exchange, 400,
                            new SimpleJson().put("error", "region and name query params are required").build());
                    return;
                }

                RouteEntry entry = routeNameService.find(region, name);
                if (entry == null) {
                    HttpUtil.sendJson(exchange, 404,
                            new SimpleJson().put("error", "Unknown route/rank: " + name + " in " + region).build());
                    return;
                }

                TimetableEntry timetable = buildTimetable(entry);
                String timesJson = "[" + String.join(",", timetable.getFixedTimes().stream()
                        .map(t -> "\"" + t + "\"").toArray(String[]::new)) + "]";

                String json = new SimpleJson()
                        .put("region", timetable.getRegion())
                        .put("name", timetable.getName())
                        .put("mode", entry.getMode().toString())
                        .putRaw("times", timesJson)
                        .put("note", timetable.getNote())
                        .build();
                HttpUtil.sendJson(exchange, 200, json);
            } catch (Exception e) {
                HttpUtil.sendJson(exchange, 500,
                        new SimpleJson().put("error", "internal error: " + e.getMessage()).build());
            }
        });
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private TimetableEntry buildTimetable(RouteEntry entry) {
        if (entry.getMode() == TransportMode.TAXI) {
            return new TimetableEntry(entry.getRegion(), entry.getName(), List.of(),
                    "Minibus taxis depart once loaded — no fixed timetable. Check /status for current loading state.");
        }
        // Bus/train: a simple fixed half-hourly timetable for the demo.
        List<String> times = Arrays.asList("06:00", "06:30", "07:00", "07:30", "08:00", "17:00", "17:30", "18:00");
        return new TimetableEntry(entry.getRegion(), entry.getName(), times, "Fixed timetable, subject to live status.");
    }
}
