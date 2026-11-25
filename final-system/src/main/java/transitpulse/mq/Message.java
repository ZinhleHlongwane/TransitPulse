package transitpulse.mq;

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable message published to a MessageQueue topic.
 */
public final class Message {
    private final String topic;
    private final String payload;
    private final Instant publishedAt;

    public Message(String topic, String payload) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.publishedAt = Instant.now();
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    @Override
    public String toString() {
        return "[" + topic + " @ " + publishedAt + "] " + payload;
    }
}
