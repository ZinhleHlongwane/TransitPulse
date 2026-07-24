package transitpulse.status;

import transitpulse.mq.MessageQueue;
import transitpulse.alertbot.AlertBot;
import transitpulse.util.SimpleJson;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watches a fixed set of routes/ranks via the (resilient) Status Service
 * client and publishes a "status-change" event to the MQ whenever a
 * watched route/rank's status differs from what was last seen.
 *
 * This is what feeds the Iteration 4 AlertBot — the monitor doesn't know
 * or care who's listening for status changes, it just publishes them.
 */
public class StatusChangeMonitor {

    private final ResilientStatusClient client;
    private final MessageQueue mq;
    private final Map<String, StatusInfo.Code> lastKnown = new ConcurrentHashMap<>();

    public StatusChangeMonitor(ResilientStatusClient client, MessageQueue mq) {
        this.client = client;
        this.mq = mq;
    }

    /** Checks one route/rank and publishes a status-change event if its status changed. */
    public void checkOnce(String region, String name) {
        Optional<StatusInfo> current = client.getStatusOrEmpty(region, name);
        if (current.isEmpty()) {
            return; // outage already handled/alerted by ResilientStatusClient
        }
        StatusInfo status = current.get();
        String key = region.toLowerCase() + "|" + name.toLowerCase();
        StatusInfo.Code previous = lastKnown.put(key, status.getCode());

        if (previous != null && previous != status.getCode()) {
            String payload = new SimpleJson()
                    .put("region", status.getRegion())
                    .put("name", status.getName())
                    .put("mode", status.getMode().toString())
                    .put("oldStatus", previous.toString())
                    .put("newStatus", status.getCode().toString())
                    .build();
            mq.publish(AlertBot.STATUS_CHANGE_TOPIC, payload);
        }
    }
}
