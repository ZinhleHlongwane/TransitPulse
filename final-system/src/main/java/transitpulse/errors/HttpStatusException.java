package transitpulse.errors;

/**
 * Represents a completed HTTP response with a non-success status.
 * This is deliberately separate from a connection-level service outage.
 */
public class HttpStatusException extends Exception {
    private final int statusCode;
    private final String responseBody;

    public HttpStatusException(int statusCode, String responseBody) {
        super("HTTP request failed with status " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody == null ? "" : responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
}
