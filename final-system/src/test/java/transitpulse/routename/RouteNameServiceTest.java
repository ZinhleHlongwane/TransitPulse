package transitpulse.routename;

import org.junit.jupiter.api.Test;
import transitpulse.model.RouteEntry;
import transitpulse.model.TransportMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteNameServiceTest {

    @Test
    void cleansAndDeduplicatesSampleRows() {
        RouteNameService service = new RouteNameService();

        service.loadAndClean(RawRouteData.sampleRows());

        assertEquals(9, service.all().size());
        RouteEntry route = service.find("gauteng", "rea vaya trunk t1");
        assertNotNull(route);
        assertEquals("Rea Vaya Trunk T1", route.getName());
        assertEquals("RV-T1", route.getCode());
        assertEquals(TransportMode.BUS, route.getMode());
    }

    @Test
    void skipsMalformedRowsAndUnrecognisedModes() {
        RouteNameService service = new RouteNameService();

        service.loadAndClean(List.of(
                "Gauteng | Valid Route | VR-1 | bus",
                "only|three|columns",
                "Gauteng | Unknown Mode | UM-1 | ferry",
                "Gauteng | Missing Code |   | train"
        ));

        assertEquals(1, service.all().size());
        assertTrue(service.exists("GAUTENG", "valid route"));
        assertFalse(service.exists("Gauteng", "Unknown Mode"));
    }

    @Test
    void returnsAnUnmodifiableViewOfEntries() {
        RouteNameService service = new RouteNameService();
        service.loadAndClean(RawRouteData.sampleRows());

        assertThrows(UnsupportedOperationException.class,
                () -> service.all().clear());
    }
}
