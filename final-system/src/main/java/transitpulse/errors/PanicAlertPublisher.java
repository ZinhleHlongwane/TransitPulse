package transitpulse.errors;

import transitpulse.mq.MessageQueue;
import transitpulse.util.SimpleJson;

import java.time.Instant;

/**
 * Iteration 3: publishes a structured panic-alert message to the
 * "panic-alerts" MQ topic whenever a service detects it cannot reach
 * a dependency, or receives repeatedly malformed data.
 *
 * This decouples "something is wrong" from "who needs to know about it" —
 * operators, dashboards, or the AlertBot in Iteration 4 can all subscribe
 * to this topic independently.
 */
public class PanicAlertPublisher {

    public static final String TOPIC = "panic-alerts";

    private final MessageQueue mq;

    public PanicAlertPublisher(MessageQueue mq) {
        this.mq = mq;
    }

    public void publishServiceUnavailable(String serviceName, ServiceUnavailableException cause) {
        publish(serviceName, "SERVICE_UNAVAILABLE", cause.getMessage());
    }

    public void publishMalformedResponse(String serviceName, MalformedResponseException cause) {
        publish(serviceName, "MALFORMED_RESPONSE", cause.getMessage());
    }

    private void publish(String serviceName, String kind, String detail) {
        String payload = new SimpleJson()
                .put("service", serviceName)
                .put("kind", kind)
                .put("detail", detail)
                .put("timestamp", Instant.now().toString())
                .build();
        mq.publish(TOPIC, payload);
    }
}
