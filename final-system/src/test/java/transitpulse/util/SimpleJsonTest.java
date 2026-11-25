package transitpulse.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleJsonTest {

    @Test
    void buildsAndReadsSimpleStringFields() {
        String json = new SimpleJson()
                .put("region", "Gauteng")
                .put("name", "Rea Vaya Trunk T1")
                .put("active", true)
                .build();

        assertEquals("Gauteng", SimpleJson.getString(json, "region"));
        assertEquals("Rea Vaya Trunk T1", SimpleJson.getString(json, "name"));
        assertTrue(json.contains("\"active\":true"));
    }

    @Test
    void escapesQuotesBackslashesAndControlCharacters() {
        String original = "Route \"A\\B\"\nNext line";
        String json = new SimpleJson().put("name", original).build();

        assertEquals(original, SimpleJson.getString(json, "name"));
    }

    @Test
    void readsStringArraysWithWhitespaceAndEscapedValues() {
        String json = "{ \"times\" : [ \"06:00\", \"07:30\", \"Gate \\\"A\\\"\" ] }";

        assertEquals(List.of("06:00", "07:30", "Gate \"A\""),
                SimpleJson.getStringArray(json, "times"));
    }

    @Test
    void serializesStringArraysWithoutManualJsonConcatenation() {
        String json = new SimpleJson()
                .put("region", "Gauteng")
                .putStrings("times", List.of("06:00", "06:30"))
                .build();

        assertEquals(List.of("06:00", "06:30"), SimpleJson.getStringArray(json, "times"));
    }

    @Test
    void returnsNullWhenStringFieldIsMissing() {
        assertNull(SimpleJson.getString("{\"region\":\"Gauteng\"}", "name"));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalArgumentException.class,
                () -> SimpleJson.getString("{\"region\":", "region"));
    }

    @Test
    void rejectsNonStringValuesInStringArray() {
        assertThrows(IllegalArgumentException.class,
                () -> SimpleJson.getStringArray("{\"times\":[\"06:00\", 630]}", "times"));
    }

    @Test
    void rejectsMissingOrNonArrayListField() {
        assertThrows(IllegalArgumentException.class,
                () -> SimpleJson.getStringArray("{\"times\":\"06:00\"}", "times"));
    }
}
