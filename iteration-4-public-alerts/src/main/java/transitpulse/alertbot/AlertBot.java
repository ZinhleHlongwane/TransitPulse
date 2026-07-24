package transitpulse.alertbot;

import transitpulse.mq.MessageQueue;
import transitpulse.util.SimpleJson;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Iteration 4: AlertBot.
 *
 * Subscribes to the "status-change" MQ topic and turns internal
 * status-change events into public-facing alerts, debouncing so a
 * route/rank that flaps between states within a short window doesn't
 * spam the public feed.
 */
public class AlertBot {

    public static final String STATUS_CHANGE_TOPIC = "status-change";

    private final PublicAlertPublisher publisher;
    private final Duration debounceWindow;
    private final Map<String, Instant> lastPostedAt = new ConcurrentHashMap<>();

    public AlertBot(MessageQueue mq, PublicAlertPublisher publisher, Duration debounceWindow) {
        this.publisher = publisher;
        this.debounceWindow = debounceWindow;
        mq.subscribe(STATUS_CHANGE_TOPIC, this::handle);
    }

    private void handle(transitpulse.mq.Message message) {
        String payload = message.getPayload();
        String region = SimpleJson.getString(payload, "region");
        String name = SimpleJson.getString(payload, "name");
        String mode = SimpleJson.getString(payload, "mode");
        String oldStatus = SimpleJson.getString(payload, "oldStatus");
        String newStatus = SimpleJson.getString(payload, "newStatus");

        if (region == null || name == null || newStatus == null) {
            return; // malformed event, nothing sensible to alert on
        }

        String key = region.toLowerCase() + "|" + name.toLowerCase();
        Instant now = Instant.now();
        Instant last = lastPostedAt.get(key);
        if (last != null && Duration.between(last, now).compareTo(debounceWindow) < 0) {
            // Within the debounce window: suppress to avoid spamming on flapping status.
            return;
        }
        lastPostedAt.put(key, now);

        String humanStatus = humanize(newStatus);
        String text = String.format("%s (%s, %s): now %s%s",
                name, region, mode, humanStatus,
                oldStatus != null ? " (was " + humanize(oldStatus) + ")" : "");
        publisher.postAlert(text);
    }

    private String humanize(String code) {
        return code.replace('_', ' ').toLowerCase();
    }
}
