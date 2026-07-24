package transitpulse.errors;

/**
 * Thrown when a dependent service cannot be reached at all
 * (connection refused, timeout, DNS failure, etc.) as opposed to
 * responding with an error status.
 */
public class ServiceUnavailableException extends Exception {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
