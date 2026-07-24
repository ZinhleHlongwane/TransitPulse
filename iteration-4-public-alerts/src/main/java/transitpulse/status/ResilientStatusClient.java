package transitpulse.status;

import transitpulse.errors.MalformedResponseException;
import transitpulse.errors.PanicAlertPublisher;
import transitpulse.errors.ServiceUnavailableException;

import java.util.Optional;

/**
 * Iteration 3: wraps StatusClient with retry-with-backoff and panic
 * alerting, so a transient failure of the Status Service degrades
 * gracefully (a stale/absent result, plus an alert) instead of crashing
 * the caller.
 */
public class ResilientStatusClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MILLIS = 100;

    private final StatusClient delegate;
    private final PanicAlertPublisher panicAlertPublisher;
    private final String serviceName = "StatusService";

    public ResilientStatusClient(StatusClient delegate, PanicAlertPublisher panicAlertPublisher) {
        this.delegate = delegate;
        this.panicAlertPublisher = panicAlertPublisher;
    }

    /**
     * Returns the status if it could be retrieved within MAX_ATTEMPTS,
     * or Optional.empty() if the dependency stayed unreachable — callers
     * can then fall back to a cached value, a "status unknown" response,
     * or simply skip this cycle, rather than crashing.
     */
    public Optional<StatusInfo> getStatusOrEmpty(String region, String name) {
        ServiceUnavailableException lastUnavailable = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return Optional.of(delegate.getStatus(region, name));
            } catch (ServiceUnavailableException e) {
                lastUnavailable = e;
                sleep(BACKOFF_MILLIS * attempt);
            } catch (MalformedResponseException e) {
                panicAlertPublisher.publishMalformedResponse(serviceName, e);
                return Optional.empty(); // malformed data won't fix itself on retry
            }
        }

        // Exhausted retries against an unreachable dependency: raise a panic alert.
        panicAlertPublisher.publishServiceUnavailable(serviceName, lastUnavailable);
        return Optional.empty();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
