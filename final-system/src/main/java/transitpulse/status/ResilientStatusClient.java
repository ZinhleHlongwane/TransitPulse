package transitpulse.status;

import transitpulse.errors.MalformedResponseException;
import transitpulse.errors.PanicAlertPublisher;
import transitpulse.errors.ServiceUnavailableException;
import transitpulse.errors.InvalidRequestException;
import transitpulse.errors.RemoteServiceException;

import java.util.Optional;

/**
 * Wraps StatusClient with retry-with-backoff and panic alerting, so a
 * transient failure of the Status Service degrades gracefully instead
 * of crashing the caller.
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
     * Returns the status after a successful request, or Optional.empty()
     * when the service remains unavailable or returns malformed data.
     *
     * If the calling thread is interrupted during backoff, retries stop
     * immediately and the interrupt flag is restored. Interruption is not
     * reported as a service outage.
     */
    public Optional<StatusInfo> getStatusOrEmpty(String region, String name) throws InvalidRequestException, RemoteServiceException, InterruptedException {
        ServiceUnavailableException lastUnavailable = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return Optional.of(delegate.getStatus(region, name));
            } catch (ServiceUnavailableException e) {
                lastUnavailable = e;
                if (attempt < MAX_ATTEMPTS && !pause(BACKOFF_MILLIS * attempt)) {
                    return Optional.empty();
                }
            } catch (MalformedResponseException e) {
                panicAlertPublisher.publishMalformedResponse(serviceName, e);
                return Optional.empty();
            }
        }

        if (lastUnavailable != null) {
            panicAlertPublisher.publishServiceUnavailable(serviceName, lastUnavailable);
        }
        return Optional.empty();
    }

    private boolean pause(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
