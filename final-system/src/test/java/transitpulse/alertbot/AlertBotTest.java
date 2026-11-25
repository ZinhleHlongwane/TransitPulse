package transitpulse.alertbot;

import org.junit.jupiter.api.Test;
import transitpulse.mq.MessageQueue;
import transitpulse.util.SimpleJson;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AlertBotTest {

    @Test
    void publishesOnlyTheLatestStatusAfterTheRouteBecomesQuiet() throws Exception {
        List<String> published = new CopyOnWriteArrayList<>();
        CountDownLatch alertPublished = new CountDownLatch(1);

        try (MessageQueue queue = new MessageQueue();
             AlertBot bot = new AlertBot(queue, alert -> {
                 published.add(alert);
                 alertPublished.countDown();
             }, Duration.ofMillis(150))) {

            publishStatus(queue, "Gauteng", "Rea Vaya", "ON_TIME", "DELAYED");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));

            publishStatus(queue, "Gauteng", "Rea Vaya", "DELAYED", "CANCELLED");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));

            publishStatus(queue, "Gauteng", "Rea Vaya", "CANCELLED", "ON_TIME");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));

            assertTrue(published.isEmpty(), "alerts should wait for the quiet period");
            assertTrue(alertPublished.await(2, TimeUnit.SECONDS));
            assertEquals(1, published.size());
            assertTrue(published.get(0).contains("now on time"));
            assertTrue(published.get(0).contains("(was cancelled)"));
        }
    }

    @Test
    void debouncesEachRouteIndependently() throws Exception {
        List<String> published = new CopyOnWriteArrayList<>();
        CountDownLatch twoAlertsPublished = new CountDownLatch(2);

        try (MessageQueue queue = new MessageQueue();
             AlertBot bot = new AlertBot(queue, alert -> {
                 published.add(alert);
                 twoAlertsPublished.countDown();
             }, Duration.ofMillis(100))) {

            publishStatus(queue, "Gauteng", "Route A", "ON_TIME", "DELAYED");
            publishStatus(queue, "Gauteng", "Route B", "ON_TIME", "CANCELLED");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));

            assertTrue(twoAlertsPublished.await(2, TimeUnit.SECONDS));
            assertEquals(2, published.size());
            assertTrue(published.stream().anyMatch(alert -> alert.contains("Route A")));
            assertTrue(published.stream().anyMatch(alert -> alert.contains("Route B")));
        }
    }

    @Test
    void closingBotCancelsPendingAlertsAndUnsubscribes() throws Exception {
        List<String> published = new CopyOnWriteArrayList<>();
        MessageQueue queue = new MessageQueue();
        AlertBot bot = new AlertBot(queue, published::add, Duration.ofMillis(200));

        publishStatus(queue, "Gauteng", "Route A", "ON_TIME", "DELAYED");
        assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));
        bot.close();

        Thread.sleep(250);
        assertTrue(published.isEmpty());

        publishStatus(queue, "Gauteng", "Route A", "DELAYED", "CANCELLED");
        assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));
        assertTrue(published.isEmpty());
        queue.close();
    }

    @Test
    void ignoresMalformedStatusChangeEvents() throws Exception {
        List<String> published = new CopyOnWriteArrayList<>();

        try (MessageQueue queue = new MessageQueue();
             AlertBot bot = new AlertBot(queue, published::add, Duration.ofMillis(50))) {

            queue.publish(AlertBot.STATUS_CHANGE_TOPIC, "{\"region\":\"Gauteng\",\"name\":\"Route A\"}");
            assertTrue(queue.awaitIdle(Duration.ofSeconds(1)));
            Thread.sleep(100);
            assertTrue(published.isEmpty());
        }
    }

    private static void publishStatus(
            MessageQueue queue, String region, String name, String oldStatus, String newStatus) {
        String payload = new SimpleJson()
                .put("region", region)
                .put("name", name)
                .put("mode", "BUS")
                .put("oldStatus", oldStatus)
                .put("newStatus", newStatus)
                .build();
        queue.publish(AlertBot.STATUS_CHANGE_TOPIC, payload);
    }
}
