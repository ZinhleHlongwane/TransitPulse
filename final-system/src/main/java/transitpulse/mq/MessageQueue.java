package transitpulse.mq;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A small in-memory, topic-based publish/subscribe queue.
 *
 * Delivery is asynchronous. Each publication is handed to every subscriber
 * registered for that topic when publish is called. This is a demonstration
 * adapter, not a durable message broker: messages are not persisted and are
 * lost when the process stops.
 *
 * A single queue may be shared by independent producers and consumers.
 */
public class MessageQueue implements AutoCloseable {

    private final Map<String, List<Consumer<Message>>> subscribers =
            new java.util.concurrent.ConcurrentHashMap<>();
    // A single worker preserves publication order for subscribers. This is\n    // important for consumers such as AlertBot that coalesce successive events.\n    private final ExecutorService deliveryPool = Executors.newSingleThreadExecutor();
    private final Object lifecycleLock = new Object();

    private int pendingDeliveries;
    private boolean closed;

    /**
     * Registers a handler for a topic.
     *
     * @return a handle that can be closed to unsubscribe the handler
     */
    public Subscription subscribe(String topic, Consumer<Message> handler) {
        String checkedTopic = requireTopic(topic);
        Objects.requireNonNull(handler, "handler must not be null");

        synchronized (lifecycleLock) {
            ensureOpen();
            List<Consumer<Message>> handlers =
                    subscribers.computeIfAbsent(checkedTopic, ignored -> new CopyOnWriteArrayList<>());
            handlers.add(handler);
        }

        return () -> {
            synchronized (lifecycleLock) {
                List<Consumer<Message>> handlers = subscribers.get(checkedTopic);
                if (handlers != null) {
                    handlers.remove(handler);
                    if (handlers.isEmpty()) {
                        subscribers.remove(checkedTopic, handlers);
                    }
                }
            }
        };
    }

    /**
     * Publishes to the subscribers registered at the time of this call.
     * The method does not wait for handlers to finish.
     *
     * @throws IllegalStateException if the queue has been shut down
     */
    public void publish(String topic, String payload) {
        String checkedTopic = requireTopic(topic);
        Message message = new Message(checkedTopic, payload);

        synchronized (lifecycleLock) {
            ensureOpen();
            List<Consumer<Message>> handlers = subscribers.get(checkedTopic);
            if (handlers == null || handlers.isEmpty()) {
                return;
            }

            // Snapshot the subscribers so a later unsubscribe does not change
            // the delivery set for a message that has already been published.
            for (Consumer<Message> handler : List.copyOf(handlers)) {
                pendingDeliveries++;
                try {
                    deliveryPool.submit(() -> deliver(handler, message));
                } catch (RejectedExecutionException exception) {
                    pendingDeliveries--;
                    lifecycleLock.notifyAll();
                    throw exception;
                }
            }
        }
    }

    private void deliver(Consumer<Message> handler, Message message) {
        try {
            handler.accept(message);
        } catch (RuntimeException exception) {
            // Isolate a failing consumer; other consumers still receive the message.
            System.err.println("MessageQueue subscriber failed for topic '"
                    + message.getTopic() + "': " + exception.getMessage());
        } finally {
            synchronized (lifecycleLock) {
                pendingDeliveries--;
                lifecycleLock.notifyAll();
            }
        }
    }

    /**
     * Waits until all deliveries published before the queue becomes idle
     * have completed, or until the timeout expires.
     *
     * @return true when there are no pending deliveries; false on timeout
     */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }

        long remainingNanos = timeout.toNanos();
        long deadline = System.nanoTime() + remainingNanos;

        synchronized (lifecycleLock) {
            while (pendingDeliveries > 0) {
                if (remainingNanos <= 0) {
                    return false;
                }

                long millis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
                int nanos = (int) (remainingNanos - TimeUnit.MILLISECONDS.toNanos(millis));
                lifecycleLock.wait(millis, nanos);
                remainingNanos = deadline - System.nanoTime();
            }
            return true;
        }
    }

    /**
     * Stops accepting new messages and lets already submitted deliveries finish.
     * Call awaitIdle before shutdown when the caller needs to know that handlers
     * have completed.
     */
    public void shutdown() {
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
            deliveryPool.shutdown();
        }
    }

    /**
     * Waits for the delivery executor to terminate after shutdown.
     */
    public boolean awaitTermination(Duration timeout) throws InterruptedException {
        Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        return deliveryPool.awaitTermination(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void close() {
        shutdown();
        try {
            if (!awaitTermination(Duration.ofSeconds(5))) {
                deliveryPool.shutdownNow();
            }
        } catch (InterruptedException exception) {
            deliveryPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("MessageQueue has been shut down");
        }
    }

    private static String requireTopic(String topic) {
        Objects.requireNonNull(topic, "topic must not be null");
        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        return topic;
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }
}
