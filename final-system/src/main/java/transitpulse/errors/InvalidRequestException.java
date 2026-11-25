package transitpulse.errors;

/** The remote service rejected the request with a 4xx HTTP response. */
public class InvalidRequestException extends Exception {
    private final int statusCode;

    public InvalidRequestException(String serviceName, int statusCode, String detail) {
        super(serviceName + " rejected the request (HTTP " + statusCode + "): " + detail);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
