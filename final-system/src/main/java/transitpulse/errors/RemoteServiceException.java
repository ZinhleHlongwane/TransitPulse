package transitpulse.errors;

/** The remote service responded with a 5xx HTTP status. */
public class RemoteServiceException extends Exception {
    private final int statusCode;

    public RemoteServiceException(String serviceName, int statusCode, String detail) {
        super(serviceName + " failed (HTTP " + statusCode + "): " + detail);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
