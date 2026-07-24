package transitpulse.mq;

import java.time.Instant;

/**
 * A single message published to a topic on the MessageQueue.
 */
public final class Message {
    private final String topic;
    private final String payload;
    private final Instant publishedAt;

    public Message(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
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
