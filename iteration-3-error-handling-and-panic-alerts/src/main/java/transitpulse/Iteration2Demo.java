package transitpulse;

import transitpulse.routename.RawRouteData;
import transitpulse.routename.RouteNameService;
import transitpulse.status.StatusClient;
import transitpulse.status.StatusInfo;
import transitpulse.status.StatusService;
import transitpulse.timetable.TimetableClient;
import transitpulse.timetable.TimetableEntry;
import transitpulse.timetable.TimetableService;

/**
 * Starts the Route-name Service, Status Service, and Timetable Service,
 * then queries all three transport modes to show inter-service communication.
 */
public class Iteration2Demo {
    public static void main(String[] args) throws Exception {
        RouteNameService routeNameService = new RouteNameService();
        routeNameService.loadAndClean(RawRouteData.sampleRows());

        StatusService statusService = new StatusService(routeNameService);
        TimetableService timetableService = new TimetableService(routeNameService);
        statusService.start(8081);
        timetableService.start(8082);

        try {
            StatusClient statusClient = new StatusClient("http://localhost:8081");
            TimetableClient timetableClient = new TimetableClient("http://localhost:8082");

            String[][] queries = {
                    {"Gauteng", "Rea Vaya Trunk T1"},      // bus
                    {"Gauteng", "Metrorail Central Line"}, // train
                    {"Gauteng", "Bree Street Taxi Rank"}   // taxi
            };

            for (String[] q : queries) {
                String region = q[0], name = q[1];
                StatusInfo status = statusClient.getStatus(region, name);
                TimetableEntry timetable = timetableClient.getTimetable(region, name);

                System.out.println("== " + name + " (" + region + ") ==");
                System.out.println("Status: " + status);
                if (timetable.getFixedTimes().isEmpty()) {
                    System.out.println("Timetable: " + timetable.getNote());
                } else {
                    System.out.println("Timetable: " + timetable.getFixedTimes());
                }
                System.out.println();
            }

            System.out.println("Querying an unknown route to show validation against Route-name Service:");
            try {
                statusClient.getStatus("Gauteng", "Nonexistent Route");
            } catch (Exception e) {
                System.out.println("(handled in Iteration 3 error handling — for now: " + e.getMessage() + ")");
            }

        } finally {
            statusService.stop();
            timetableService.stop();
        }
    }
}
