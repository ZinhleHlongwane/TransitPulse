package transitpulse.errors;

import org.junit.jupiter.api.Test;
import transitpulse.mq.Message;
import transitpulse.mq.MessageQueue;
import transitpulse.util.SimpleJson;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PanicAlertPublisherTest {

    @Test
    void publishesStructuredServiceUnavailableEventToPanicTopic() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            AtomicReference<Message> received = new AtomicReference<>();
            queue.subscribe(PanicAlertPublisher.TOPIC, received::set);
            PanicAlertPublisher publisher = new PanicAlertPublisher(queue);

            publisher.publishServiceUnavailable(
                    "StatusService", new ServiceUnavailableException("connection refused"));

            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertNotNull(received.get());
            assertEquals(PanicAlertPublisher.TOPIC, received.get().getTopic());
            assertEquals("StatusService", SimpleJson.getString(received.get().getPayload(), "service"));
            assertEquals("SERVICE_UNAVAILABLE", SimpleJson.getString(received.get().getPayload(), "kind"));
            assertEquals("connection refused", SimpleJson.getString(received.get().getPayload(), "detail"));
        }
    }

    @Test
    void publishesMalformedResponseEventToPanicTopic() throws Exception {
        try (MessageQueue queue = new MessageQueue()) {
            AtomicReference<Message> received = new AtomicReference<>();
            queue.subscribe(PanicAlertPublisher.TOPIC, received::set);
            PanicAlertPublisher publisher = new PanicAlertPublisher(queue);

            publisher.publishMalformedResponse(
                    "TimetableService", new MalformedResponseException("missing departure time"));

            assertTrue(queue.awaitIdle(Duration.ofSeconds(2)));
            assertNotNull(received.get());
            assertEquals("MALFORMED_RESPONSE", SimpleJson.getString(received.get().getPayload(), "kind"));
            assertEquals("TimetableService", SimpleJson.getString(received.get().getPayload(), "service"));
        }
    }
}
