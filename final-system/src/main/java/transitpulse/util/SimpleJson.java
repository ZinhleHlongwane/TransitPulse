package transitpulse.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON object builder/parser for TransitPulse's own flat
 * request/response payloads. Deliberately dependency-free (no Jackson/Gson)
 * so the whole project compiles and runs with just the JDK.
 *
 * Only supports flat string-keyed objects with string/number/boolean values,
 * which is all these services need.
 */
public final class SimpleJson {

    private final Map<String, String> fields = new LinkedHashMap<>();

    public SimpleJson put(String key, String value) {
        fields.put(key, "\"" + escape(value) + "\"");
        return this;
    }

    public SimpleJson putRaw(String key, String rawValue) {
        // for numbers, booleans, or nested JSON already built
        fields.put(key, rawValue);
        return this;
    }

    public SimpleJson put(String key, boolean value) {
        fields.put(key, Boolean.toString(value));
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":").append(e.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Extracts the string value for a given key from a flat JSON object string.
     * Not a general-purpose parser — just enough for this project's own responses.
     */
    public static String getString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start == -1) return null;
        start += needle.length();
        int end = json.indexOf('"', start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
