package transitpulse.errors;

/**
 * Thrown when a dependent service responded, but the response body
 * could not be parsed into the expected shape.
 */
public class MalformedResponseException extends Exception {
    public MalformedResponseException(String message) {
        super(message);
    }
}
