package transitpulse.status;

import org.junit.jupiter.api.Test;
import transitpulse.errors.PanicAlertPublisher;
import transitpulse.errors.ServiceUnavailableException;
import transitpulse.mq.MessageQueue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResilientStatusClientTest {

    @Test
    void stopsRetryingWhenInterruptedAndRestoresInterruptFlag() {
        AtomicInteger attempts = new AtomicInteger();
        RecordingMessageQueue queue = new RecordingMessageQueue();
        StatusClient unavailableClient = new StatusClient("http://unavailable") {
            @Override
            public StatusInfo getStatus(String region, String name) throws ServiceUnavailableException {
                attempts.incrementAndGet();
                throw new ServiceUnavailableException("service unavailable");
            }
        };
        ResilientStatusClient client =
                new ResilientStatusClient(unavailableClient, new PanicAlertPublisher(queue));

        Thread.currentThread().interrupt();
        try {
            Optional<StatusInfo> result = client.getStatusOrEmpty("Gauteng", "Route");

            assertTrue(result.isEmpty());
            assertEquals(1, attempts.get(), "interruption should stop further attempts");
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt status should be restored");
            assertEquals(0, queue.publishedMessages.get(),
                    "interruption should not be reported as a service outage");
        } finally {
            Thread.interrupted();
            queue.shutdown();
        }
    }

    @Test
    void publishesOnePanicAlertAfterAllAttemptsFail() {
        AtomicInteger attempts = new AtomicInteger();
        RecordingMessageQueue queue = new RecordingMessageQueue();
        StatusClient unavailableClient = new StatusClient("http://unavailable") {
            @Override
            public StatusInfo getStatus(String region, String name) throws ServiceUnavailableException {
                attempts.incrementAndGet();
                throw new ServiceUnavailableException("service unavailable");
            }
        };
        ResilientStatusClient client =
                new ResilientStatusClient(unavailableClient, new PanicAlertPublisher(queue));

        Optional<StatusInfo> result = client.getStatusOrEmpty("Gauteng", "Route");

        assertTrue(result.isEmpty());
        assertEquals(3, attempts.get());
        assertEquals(1, queue.publishedMessages.get());
        queue.shutdown();
    }

    private static final class RecordingMessageQueue extends MessageQueue {
        private final AtomicInteger publishedMessages = new AtomicInteger();

        @Override
        public void publish(String topic, String payload) {
            publishedMessages.incrementAndGet();
        }
    }
}
