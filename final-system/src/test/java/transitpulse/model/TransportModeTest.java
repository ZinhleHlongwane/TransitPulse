package transitpulse.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransportModeTest {

    @Test
    void parsesSupportedRawTransportNames() {
        assertEquals(TransportMode.BUS, TransportMode.fromRaw(" bus "));
        assertEquals(TransportMode.TRAIN, TransportMode.fromRaw("TRAIN"));
        assertEquals(TransportMode.TAXI, TransportMode.fromRaw("minibus taxi"));
        assertEquals(TransportMode.TAXI, TransportMode.fromRaw("Minibus"));
    }

    @Test
    void rejectsNullAndUnsupportedModes() {
        assertThrows(IllegalArgumentException.class, () -> TransportMode.fromRaw(null));
        assertThrows(IllegalArgumentException.class, () -> TransportMode.fromRaw("ferry"));
    }
}
