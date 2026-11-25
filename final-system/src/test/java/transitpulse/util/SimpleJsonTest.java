package transitpulse.util;

import org.junit.jupiter.api.Test;

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
    void returnsNullWhenStringFieldIsMissing() {
        assertNull(SimpleJson.getString("{\"region\":\"Gauteng\"}", "name"));
    }
}
