package transitpulse;

import transitpulse.alertbot.AlertBot;
import transitpulse.alertbot.ConsolePublicAlertPublisher;
import transitpulse.errors.PanicAlertPublisher;
import transitpulse.mq.MessageQueue;
import transitpulse.routename.RawRouteData;
import transitpulse.routename.RouteNameService;
import transitpulse.status.ResilientStatusClient;
import transitpulse.status.StatusChangeMonitor;
import transitpulse.status.StatusClient;
import transitpulse.status.StatusInfo;
import transitpulse.status.StatusService;

import java.time.Duration;

/**
 * Full end-to-end demo: Route-name Service -> Status Service -> MQ-based
 * status-change monitoring -> AlertBot -> public alert feed, including a
 * demonstration of debouncing against a rapidly flapping status.
 */
public class Iteration4Demo {
    public static void main(String[] args) throws Exception {
        RouteNameService routeNameService = new RouteNameService();
        routeNameService.loadAndClean(RawRouteData.sampleRows());

        StatusService statusService = new StatusService(routeNameService);
        statusService.start(8081);

        MessageQueue mq = new MessageQueue();

        // Panic alerts still flow, same as Iteration 3.
        mq.subscribe(PanicAlertPublisher.TOPIC, msg ->
                System.out.println(">>> [panic-alerts] " + msg.getPayload()));
        PanicAlertPublisher panicAlertPublisher = new PanicAlertPublisher(mq);

        // AlertBot subscribes to status-change and posts publicly, debounced to 1 second for this demo.
        AlertBot alertBot = new AlertBot(mq, new ConsolePublicAlertPublisher(), Duration.ofSeconds(1));

        StatusClient rawClient = new StatusClient("http://localhost:8081");
        ResilientStatusClient resilientClient = new ResilientStatusClient(rawClient, panicAlertPublisher);
        StatusChangeMonitor monitor = new StatusChangeMonitor(resilientClient, mq);

        String region = "Gauteng";
        String route = "Rea Vaya Trunk T1";

        System.out.println("-- Establishing baseline status --");
        monitor.checkOnce(region, route); // first check just records the baseline, no event yet
        Thread.sleep(100);

        System.out.println("-- Forcing a real status change: ON_TIME -> DELAYED --");
        statusService.forceStatus(region, route, StatusInfo.Code.DELAYED);
        monitor.checkOnce(region, route);
        Thread.sleep(200); // let async MQ delivery run

        System.out.println();
        System.out.println("-- Simulating flapping status (should be debounced to one public alert) --");
        statusService.forceStatus(region, route, StatusInfo.Code.ON_TIME);
        monitor.checkOnce(region, route);
        statusService.forceStatus(region, route, StatusInfo.Code.DELAYED);
        monitor.checkOnce(region, route);
        statusService.forceStatus(region, route, StatusInfo.Code.ON_TIME);
        monitor.checkOnce(region, route);
        Thread.sleep(300);

        System.out.println();
        System.out.println("-- Waiting past the debounce window, then changing status again --");
        Thread.sleep(1000);
        statusService.forceStatus(region, route, StatusInfo.Code.CANCELLED);
        monitor.checkOnce(region, route);
        Thread.sleep(200);

        statusService.stop();
        mq.shutdown();
    }
}
