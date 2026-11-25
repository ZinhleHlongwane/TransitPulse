package transitpulse.status;

import transitpulse.errors.MalformedResponseException;
import transitpulse.errors.HttpStatusException;
import transitpulse.errors.InvalidRequestException;
import transitpulse.errors.RemoteServiceException;
import transitpulse.errors.ServiceUnavailableException;
import transitpulse.model.TransportMode;
import transitpulse.util.HttpUtil;
import transitpulse.util.SimpleJson;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client for querying the Status Service over HTTP.
 * Translates low-level network failures and bad payloads into the
 * project's own exception types so callers have a single, consistent
 * error-handling contract.
 */
public class StatusClient {

    private final String baseUrl;

    public StatusClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public StatusInfo getStatus(String region, String name) throws ServiceUnavailableException, MalformedResponseException, InvalidRequestException, RemoteServiceException, InterruptedException {
        String url = baseUrl + "/status?region=" + encode(region) + "&name=" + encode(name);
        String body;
        try {
            body = HttpUtil.get(url);
        } catch (HttpStatusException e) {
            if (e.isClientError()) {
                throw new InvalidRequestException("Status Service", e.getStatusCode(), e.getResponseBody());
            }
            throw new RemoteServiceException("Status Service", e.getStatusCode(), e.getResponseBody());
        } catch (IOException e) {
            throw new ServiceUnavailableException("Could not reach Status Service at " + baseUrl, e);
        }

        String regionField = SimpleJson.getString(body, "region");
        String nameField = SimpleJson.getString(body, "name");
        String modeField = SimpleJson.getString(body, "mode");
        String statusField = SimpleJson.getString(body, "status");
        String messageField = SimpleJson.getString(body, "message");

        if (regionField == null || nameField == null || modeField == null || statusField == null) {
            throw new MalformedResponseException("Status Service response missing expected fields: " + body);
        }

        TransportMode mode;
        StatusInfo.Code code;
        try {
            mode = TransportMode.valueOf(modeField);
            code = StatusInfo.Code.valueOf(statusField);
        } catch (IllegalArgumentException e) {
            throw new MalformedResponseException("Status Service returned an unrecognized mode/status: " + body);
        }

        return new StatusInfo(regionField, nameField, mode, code, messageField);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
