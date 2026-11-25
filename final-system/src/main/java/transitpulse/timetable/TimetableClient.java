package transitpulse.timetable;

import transitpulse.errors.MalformedResponseException;
import transitpulse.errors.ServiceUnavailableException;
import transitpulse.util.HttpUtil;
import transitpulse.util.SimpleJson;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TimetableClient {

    private final String baseUrl;

    public TimetableClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public TimetableEntry getTimetable(String region, String name) throws ServiceUnavailableException, MalformedResponseException {
        String url = baseUrl + "/timetable?region=" + encode(region) + "&name=" + encode(name);
        String body;
        try {
            body = HttpUtil.get(url);
        } catch (IOException | InterruptedException e) {
            throw new ServiceUnavailableException("Could not reach Timetable Service at " + baseUrl, e);
        }

        String regionField = SimpleJson.getString(body, "region");
        String nameField = SimpleJson.getString(body, "name");
        String note = SimpleJson.getString(body, "note");
        List<String> times = extractTimes(body);

        if (regionField == null || nameField == null) {
            throw new MalformedResponseException("Timetable Service response missing expected fields: " + body);
        }

        return new TimetableEntry(regionField, nameField, times, note);
    }

    private List<String> extractTimes(String json) {
        List<String> times = new ArrayList<>();
        int start = json.indexOf("\"times\":[");
        if (start == -1) return times;
        start += "\"times\":[".length();
        int end = json.indexOf(']', start);
        if (end == -1) return times;
        String inner = json.substring(start, end).trim();
        if (inner.isEmpty()) return times;
        for (String t : inner.split(",")) {
            times.add(t.trim().replace("\"", ""));
        }
        return times;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
