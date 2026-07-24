package transitpulse;

import transitpulse.model.RouteEntry;
import transitpulse.routename.RawRouteData;
import transitpulse.routename.RouteNameService;

import java.util.List;

/**
 * Runs the Route-name Service against messy sample data and prints
 * the cleaned, de-duplicated result.
 */
public class Iteration1Demo {
    public static void main(String[] args) {
        RouteNameService service = new RouteNameService();
        List<String> rawRows = RawRouteData.sampleRows();

        System.out.println("Raw rows loaded: " + rawRows.size());
        service.loadAndClean(rawRows);

        List<RouteEntry> cleaned = service.all();
        System.out.println("Cleaned, de-duplicated entries: " + cleaned.size());
        System.out.println();
        for (RouteEntry e : cleaned) {
            System.out.printf("[%s] %-28s %-10s (%s)%n",
                    e.getMode(), e.getName(), e.getCode(), e.getRegion());
        }

        System.out.println();
        System.out.println("Lookup 'Gauteng' / 'Bree Street Taxi Rank': "
                + service.find("gauteng", "bree street taxi rank"));
    }
}
