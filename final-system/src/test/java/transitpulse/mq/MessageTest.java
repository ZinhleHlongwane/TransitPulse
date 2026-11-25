package transitpulse.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void storesTopicPayloadAndPublicationTimestamp() {
        Message message = new Message("status-change", "DELAYED");

        assertEquals("status-change", message.getTopic());
        assertEquals("DELAYED", message.getPayload());
        assertNotNull(message.getPublishedAt());
    }

    @Test
    void rejectsInvalidTopicAndNullPayload() {
        assertThrows(IllegalArgumentException.class, () -> new Message(" ", "payload"));
        assertThrows(NullPointerException.class, () -> new Message(null, "payload"));
        assertThrows(NullPointerException.class, () -> new Message("events", null));
    }
}
