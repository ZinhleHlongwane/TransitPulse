package transitpulse.mq;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * A minimal in-memory Message Queue: topic-based publish/subscribe with
 * asynchronous delivery.
 *
 * This stands in for a real broker (RabbitMQ/ActiveMQ) so the whole
 * project stays dependency-free and runnable offline. The public API
 * (publish/subscribe by topic, async delivery, decoupled producers and
 * consumers) mirrors how a real MQ would be used here — swapping this
 * class out for a real broker client later would not require changing
 * any of the calling code's logic.
 */
public class MessageQueue {

    private final Map<String, List<Consumer<Message>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService deliveryPool = Executors.newCachedThreadPool();

    public void subscribe(String topic, Consumer<Message> handler) {
        subscribers.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /**
     * Publishes a message and returns immediately — delivery to subscribers
     * happens asynchronously, so a slow or failing subscriber never blocks
     * the publisher (this is the core resilience benefit of using an MQ).
     */
    public void publish(String topic, String payload) {
        Message message = new Message(topic, payload);
        List<Consumer<Message>> handlers = subscribers.getOrDefault(topic, List.of());
        for (Consumer<Message> handler : handlers) {
            deliveryPool.submit(() -> {
                try {
                    handler.accept(message);
                } catch (Exception e) {
                    // A misbehaving subscriber must not take down the queue or other subscribers.
                    System.err.println("Subscriber for topic '" + topic + "' threw an exception: " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() {
        deliveryPool.shutdown();
    }
}
