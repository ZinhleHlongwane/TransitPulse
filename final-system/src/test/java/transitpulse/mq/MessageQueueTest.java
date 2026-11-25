package transitpulse.mq;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MessageQueueTest {

    @Test
    void deliversPublishedMessageToEverySubscriberOfItsTopic() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            List<String> received = new CopyOnWriteArrayList<>();
            queue.subscribe("status-change", message -> received.add("first:" + message.getPayload()));
            queue.subscribe("status-change", message -> received.add("second:" + message.getPayload()));
            queue.subscribe("panic-alerts", message -> received.add("panic:" + message.getPayload()));

            queue.publish("status-change", "DELAYED");

            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertEquals(2, received.size());
            assertTrue(received.contains("first:DELAYED"));
            assertTrue(received.contains("second:DELAYED"));
        }
    }

    @Test
    void subscriberFailureDoesNotPreventOtherSubscribersFromReceivingMessage() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            AtomicInteger successfulDeliveries = new AtomicInteger();
            queue.subscribe("events", message -> {
                throw new IllegalStateException("subscriber failed");
            });
            queue.subscribe("events", message -> successfulDeliveries.incrementAndGet());

            queue.publish("events", "payload");

            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertEquals(1, successfulDeliveries.get());
        }
    }

    @Test
    void subscriptionCanBeClosedWithoutReceivingFutureMessages() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            AtomicInteger deliveries = new AtomicInteger();
            MessageQueue.Subscription subscription =
                    queue.subscribe("events", message -> deliveries.incrementAndGet());

            subscription.close();
            queue.publish("events", "after unsubscribe");

            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertEquals(0, deliveries.get());
        }
    }

    @Test
    void awaitIdleTimesOutWhileSubscriberIsStillWorking() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            queue.subscribe("slow", message -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });

            queue.publish("slow", "payload");
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(queue.awaitIdle(Duration.ofMillis(20)));

            release.countDown();
            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
        }
    }

    @Test
    void rejectsPublishingAfterShutdown() {
        MessageQueue queue = new MessageQueue();
        queue.shutdown();

        assertThrows(IllegalStateException.class, () -> queue.publish("events", "late"));
        assertThrows(IllegalStateException.class, () -> queue.subscribe("events", message -> { }));
    }

    @Test
    void rejectsInvalidTopicsAndNullHandlers() {
        try (MessageQueue queue = new MessageQueue()) {
            assertThrows(IllegalArgumentException.class, () -> queue.publish(" ", "payload"));
            assertThrows(NullPointerException.class, () -> queue.publish(null, "payload"));
            assertThrows(NullPointerException.class, () -> queue.subscribe("events", null));
        }
    }
}
