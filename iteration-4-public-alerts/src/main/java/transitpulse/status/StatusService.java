package transitpulse.status;

import com.sun.net.httpserver.HttpServer;
import transitpulse.model.RouteEntry;
import transitpulse.model.TransportMode;
import transitpulse.routename.RouteNameService;
import transitpulse.util.HttpUtil;
import transitpulse.util.SimpleJson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iteration 2: Status Service.
 *
 * Exposes GET /status?region=..&name=..
 * Validates the route/rank against the Route-name Service, then returns
 * a mode-appropriate status: on-time/delayed/cancelled for bus/train,
 * loading/departing/full for taxi.
 */
public class StatusService {

    private final RouteNameService routeNameService;
    private final Random random = new Random();
    private HttpServer server;

    // In-memory "current status" per route/rank so repeated queries are stable
    // within a run, but can be nudged (e.g. by a demo) to simulate a change.
    private final Map<String, StatusInfo.Code> currentStatus = new ConcurrentHashMap<>();

    public StatusService(RouteNameService routeNameService) {
        this.routeNameService = routeNameService;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/status", exchange -> {
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

                StatusInfo status = currentStatusFor(entry);
                String json = new SimpleJson()
                        .put("region", status.getRegion())
                        .put("name", status.getName())
                        .put("mode", status.getMode().toString())
                        .put("status", status.getCode().toString())
                        .put("message", status.getMessage())
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

    /**
     * Returns the current status for an entry, generating a plausible one
     * on first query and holding it stable thereafter (unless forced to change).
     */
    private StatusInfo currentStatusFor(RouteEntry entry) {
        StatusInfo.Code code = currentStatus.computeIfAbsent(entry.identityKey(), k -> randomInitialCode(entry.getMode()));
        return toStatusInfo(entry, code);
    }

    private StatusInfo.Code randomInitialCode(TransportMode mode) {
        if (mode == TransportMode.TAXI) {
            StatusInfo.Code[] options = {StatusInfo.Code.LOADING, StatusInfo.Code.DEPARTING};
            return options[random.nextInt(options.length)];
        } else {
            // mostly on-time, occasionally delayed
            return random.nextInt(10) < 8 ? StatusInfo.Code.ON_TIME : StatusInfo.Code.DELAYED;
        }
    }

    /**
     * Test/demo hook: force a route/rank's status to a specific code,
     * used to simulate a status change for the Iteration 4 AlertBot demo.
     */
    public void forceStatus(String region, String name, StatusInfo.Code code) {
        RouteEntry entry = routeNameService.find(region, name);
        if (entry != null) {
            currentStatus.put(entry.identityKey(), code);
        }
    }

    private StatusInfo toStatusInfo(RouteEntry entry, StatusInfo.Code code) {
        String message;
        switch (code) {
            case ON_TIME: message = "Running on time"; break;
            case DELAYED: message = "Delayed by approximately 15 minutes"; break;
            case CANCELLED: message = "Service cancelled"; break;
            case LOADING: message = "Taxi is loading passengers"; break;
            case DEPARTING: message = "Taxi is full and about to depart"; break;
            case FULL: message = "Rank is at capacity, next taxi shortly"; break;
            default: message = "Unknown status";
        }
        return new StatusInfo(entry.getRegion(), entry.getName(), entry.getMode(), code, message);
    }
}
