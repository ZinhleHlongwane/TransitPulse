package transitpulse;

import org.junit.jupiter.api.Test;
import transitpulse.alertbot.AlertBot;
import transitpulse.errors.InvalidRequestException;
import transitpulse.errors.PanicAlertPublisher;
import transitpulse.mq.Message;
import transitpulse.mq.MessageQueue;
import transitpulse.routename.RawRouteData;
import transitpulse.routename.RouteNameService;
import transitpulse.status.ResilientStatusClient;
import transitpulse.status.StatusChangeMonitor;
import transitpulse.status.StatusClient;
import transitpulse.status.StatusInfo;
import transitpulse.status.StatusService;
import transitpulse.timetable.TimetableClient;
import transitpulse.timetable.TimetableService;
import transitpulse.util.SimpleJson;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TransitPulseIntegrationTest {

    private static final String REGION = "Gauteng";
    private static final String ROUTE = "Rea Vaya Trunk T1";

    @Test
    void routeDataFlowsThroughStatusAndTimetableHttpServices() throws Exception {
        RouteNameService routes = new RouteNameService();
        routes.loadAndClean(RawRouteData.sampleRows());

        StatusService statusService = new StatusService(routes);
        TimetableService timetableService = new TimetableService(routes);
        statusService.start(0);
        timetableService.start(0);

        try {
            var status = new StatusClient("http://localhost:" + statusService.getPort())
                    .getStatus(REGION, ROUTE);
            var timetable = new TimetableClient("http://localhost:" + timetableService.getPort())
                    .getTimetable(REGION, ROUTE);

            assertEquals(REGION, status.getRegion());
            assertEquals(ROUTE, status.getName());
            assertEquals(REGION, timetable.getRegion());
            assertEquals(ROUTE, timetable.getName());
            assertFalse(timetable.getFixedTimes().isEmpty());
        } finally {
            statusService.stop();
            timetableService.stop();
        }
    }

    @Test
    void statusChangeTravelsThroughMonitorQueueAndAlertBot() throws Exception {
        RouteNameService routes = new RouteNameService();
        routes.loadAndClean(RawRouteData.sampleRows());
        StatusService service = new StatusService(routes);
        service.start(0);

        List<String> alerts = new CopyOnWriteArrayList<>();
        CountDownLatch alertPublished = new CountDownLatch(1);

        try (MessageQueue queue = new MessageQueue();
             AlertBot bot = new AlertBot(queue, alert -> {
                 alerts.add(alert);
                 alertPublished.countDown();
             }, Duration.ofMillis(100))) {

            StatusClient statusClient = new StatusClient("http://localhost:" + service.getPort());
            ResilientStatusClient resilient = new ResilientStatusClient(
                    statusClient, new PanicAlertPublisher(queue));
            StatusChangeMonitor monitor = new StatusChangeMonitor(resilient, queue);

            service.forceStatus(REGION, ROUTE, StatusInfo.Code.ON_TIME);
            monitor.checkOnce(REGION, ROUTE); // baseline: no event
            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertTrue(alerts.isEmpty());

            service.forceStatus(REGION, ROUTE, StatusInfo.Code.DELAYED);
            monitor.checkOnce(REGION, ROUTE);
            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));

            assertTrue(alertPublished.await(2, TimeUnit.SECONDS));
            assertEquals(1, alerts.size());
            assertTrue(alerts.get(0).contains("now delayed"));
            assertTrue(alerts.get(0).contains("(was on time)"));
        } finally {
            service.stop();
        }
    }

    @Test
    void exhaustedConnectionRetriesPublishOnePanicAlert() throws Exception {
        int unavailablePort;
        // Reserve an ephemeral port briefly, then release it so the client gets connection refused.
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            unavailablePort = socket.getLocalPort();
        }

        try (MessageQueue queue = new MessageQueue()) {
            AtomicReference<Message> panic = new AtomicReference<>();
            CountDownLatch panicReceived = new CountDownLatch(1);
            queue.subscribe(PanicAlertPublisher.TOPIC, message -> {
                panic.set(message);
                panicReceived.countDown();
            });

            ResilientStatusClient resilient = new ResilientStatusClient(
                    new StatusClient("http://127.0.0.1:" + unavailablePort),
                    new PanicAlertPublisher(queue));
            StatusChangeMonitor monitor = new StatusChangeMonitor(resilient, queue);

            monitor.checkOnce(REGION, ROUTE);

            assertTrue(panicReceived.await(3, TimeUnit.SECONDS));
            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertNotNull(panic.get());
            assertEquals("SERVICE_UNAVAILABLE",
                    SimpleJson.getString(panic.get().getPayload(), "kind"));
            assertEquals("StatusService",
                    SimpleJson.getString(panic.get().getPayload(), "service"));
        }
    }

    @Test
    void unknownRouteIsRejectedWithoutPublishingPanicAlert() throws Exception {
        RouteNameService routes = new RouteNameService();
        routes.loadAndClean(RawRouteData.sampleRows());
        StatusService service = new StatusService(routes);
        service.start(0);

        try (MessageQueue queue = new MessageQueue()) {
            AtomicReference<Message> panic = new AtomicReference<>();
            queue.subscribe(PanicAlertPublisher.TOPIC, panic::set);

            StatusChangeMonitor monitor = new StatusChangeMonitor(
                    new ResilientStatusClient(
                            new StatusClient("http://localhost:" + service.getPort()),
                            new PanicAlertPublisher(queue)),
                    queue);

            assertThrows(InvalidRequestException.class,
                    () -> monitor.checkOnce(REGION, "Route That Does Not Exist"));
            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertNull(panic.get(), "a 404 is a rejected lookup, not a service outage");
        } finally {
            service.stop();
        }
    }
}
