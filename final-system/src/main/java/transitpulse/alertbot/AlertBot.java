package transitpulse.alertbot;

import transitpulse.mq.Message;
import transitpulse.mq.MessageQueue;
import transitpulse.util.SimpleJson;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Converts route status-change events into public alerts.
 *
 * Debouncing means the alert is sent only after a route has received no
 * further status changes for the configured quiet period. If another
 * change arrives during that period, the previous pending alert is
 * cancelled and replaced with the newest state.
 */
public final class AlertBot implements AutoCloseable {

    public static final String STATUS_CHANGE_TOPIC = "status-change";

    private final PublicAlertPublisher publisher;
    private final Duration debounceWindow;
    private final MessageQueue.Subscription subscription;
    private final ScheduledExecutorService scheduler;
    private final Map<String, PendingAlert> pendingAlerts = new ConcurrentHashMap<>();
    private final AtomicLong nextAlertId = new AtomicLong();
    private boolean closed;

    public AlertBot(MessageQueue mq, PublicAlertPublisher publisher, Duration debounceWindow) {
        Objects.requireNonNull(mq, "message queue must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.debounceWindow = Objects.requireNonNull(debounceWindow, "debounce window must not be null");
        if (debounceWindow.isNegative()) {
            throw new IllegalArgumentException("debounce window must not be negative");
        }

        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task, "transitpulse-alertbot");
            thread.setDaemon(true);
            return thread;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.subscription = mq.subscribe(STATUS_CHANGE_TOPIC, this::handle);
    }

    private void handle(Message message) {
        String payload = message.getPayload();
        String region = SimpleJson.getString(payload, "region");
        String name = SimpleJson.getString(payload, "name");
        String mode = SimpleJson.getString(payload, "mode");
        String oldStatus = SimpleJson.getString(payload, "oldStatus");
        String newStatus = SimpleJson.getString(payload, "newStatus");

        if (region == null || name == null || newStatus == null) {
            return;
        }

        String key = (region + "|" + name).toLowerCase(Locale.ROOT);
        String alertText = String.format("%s (%s, %s): now %s%s",
                name, region, mode == null ? "transport" : mode,
                humanize(newStatus),
                oldStatus == null ? "" : " (was " + humanize(oldStatus) + ")");

        synchronized (this) {
            if (closed) {
                return;
            }

            PendingAlert previous = pendingAlerts.remove(key);
            if (previous != null) {
                previous.future().cancel(false);
            }

            long alertId = nextAlertId.incrementAndGet();
            ScheduledFuture<?> future = scheduler.schedule(
                    () -> publishIfStillPending(key, alertId, alertText),
                    debounceWindow.toNanos(),
                    TimeUnit.NANOSECONDS);
            pendingAlerts.put(key, new PendingAlert(alertId, alertText, future));
        }
    }

    private void publishIfStillPending(String key, long alertId, String alertText) {
        synchronized (this) {
            PendingAlert current = pendingAlerts.get(key);
            if (closed || current == null || current.id() != alertId) {
                return;
            }
            pendingAlerts.remove(key);
        }

        publisher.postAlert(alertText);
    }

    private String humanize(String code) {
        return code.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    /**
     * Cancels alerts that have not reached the end of their quiet period
     * and unsubscribes from future status-change events.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        subscription.close();
        for (PendingAlert alert : pendingAlerts.values()) {
            alert.future().cancel(false);
        }
        pendingAlerts.clear();
        scheduler.shutdownNow();
    }

    private record PendingAlert(long id, String text, ScheduledFuture<?> future) { }
}
