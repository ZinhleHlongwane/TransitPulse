package transitpulse.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Small JSON helper backed by Jackson. Keeps the project's convenient
 * object-builder API while using a real parser for response payloads.
 */
public final class SimpleJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ObjectNode object = MAPPER.createObjectNode();

    public SimpleJson put(String key, String value) {
        if (value == null) {
            object.putNull(key);
        } else {
            object.put(key, value);
        }
        return this;
    }

    public SimpleJson putRaw(String key, String rawValue) {
        try {
            object.set(key, MAPPER.readTree(rawValue));
            return this;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON value for field '" + key + "'", e);
        }
    }

    public SimpleJson put(String key, boolean value) {
        object.put(key, value);
        return this;
    }

    public SimpleJson putStrings(String key, List<String> values) {
        ArrayNode array = object.putArray(key);
        values.forEach(array::add);
        return this;
    }

    public String build() {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize JSON object", e);
        }
    }

    /**
     * Returns a textual field from a JSON object. Missing or non-text fields
     * return null; invalid JSON throws IllegalArgumentException.
     */
    public static String getString(String json, String key) {
        JsonNode value = readObject(json).get(key);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /**
     * Reads an array of strings. A missing field, non-array field or array
     * containing non-string values is rejected instead of silently ignored.
     */
    public static List<String> getStringArray(String json, String key) {
        JsonNode value = readObject(json).get(key);
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("Expected JSON array field '" + key + "'");
        }

        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Expected every item in '" + key + "' to be a string");
            }
            result.add(item.asText());
        }
        return List.copyOf(result);
    }

    private static JsonNode readObject(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || !root.isObject()) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON response", e);
        }
    }
}
