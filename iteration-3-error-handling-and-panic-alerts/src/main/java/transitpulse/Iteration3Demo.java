package transitpulse;

import transitpulse.errors.PanicAlertPublisher;
import transitpulse.mq.MessageQueue;
import transitpulse.routename.RawRouteData;
import transitpulse.routename.RouteNameService;
import transitpulse.status.ResilientStatusClient;
import transitpulse.status.StatusClient;
import transitpulse.status.StatusService;

import java.util.Optional;

/**
 * Demonstrates fault tolerance: a working query, then the Status Service
 * is deliberately stopped to simulate an outage. The resilient client
 * retries, ultimately gives up gracefully, and a panic alert is published
 * to the MQ instead of the caller crashing.
 */
public class Iteration3Demo {
    public static void main(String[] args) throws Exception {
        RouteNameService routeNameService = new RouteNameService();
        routeNameService.loadAndClean(RawRouteData.sampleRows());

        StatusService statusService = new StatusService(routeNameService);
        statusService.start(8081);

        MessageQueue mq = new MessageQueue();
        mq.subscribe(PanicAlertPublisher.TOPIC, msg ->
                System.out.println(">>> [panic-alerts subscriber] received: " + msg.getPayload()));

        PanicAlertPublisher panicAlertPublisher = new PanicAlertPublisher(mq);
        StatusClient rawClient = new StatusClient("http://localhost:8081");
        ResilientStatusClient resilientClient = new ResilientStatusClient(rawClient, panicAlertPublisher);

        System.out.println("-- Query while Status Service is healthy --");
        Optional<transitpulse.status.StatusInfo> result =
                resilientClient.getStatusOrEmpty("Gauteng", "Rea Vaya Trunk T1");
        System.out.println("Result: " + result.map(Object::toString).orElse("(unavailable)"));

        System.out.println();
        System.out.println("-- Simulating an outage: stopping the Status Service --");
        statusService.stop();

        Optional<transitpulse.status.StatusInfo> failedResult =
                resilientClient.getStatusOrEmpty("Gauteng", "Rea Vaya Trunk T1");
        System.out.println("Result during outage: " + failedResult.map(Object::toString).orElse("(unavailable — degraded gracefully)"));

        // Give the async MQ delivery a moment to run before the JVM exits.
        Thread.sleep(300);
        mq.shutdown();
    }
}
