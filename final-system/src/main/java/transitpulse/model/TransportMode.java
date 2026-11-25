package transitpulse.model;

/**
 * The three transport modes covered by TransitPulse.
 */
public enum TransportMode {
    BUS,
    TRAIN,
    TAXI;

    /**
     * Parses a mode from a loosely-formatted raw string
     * (e.g. "bus", "TRAIN", " Taxi ", "minibus").
     */
    public static TransportMode fromRaw(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("mode cannot be null");
        }
        String cleaned = raw.trim().toLowerCase();
        if (cleaned.equals("bus")) return BUS;
        if (cleaned.equals("train")) return TRAIN;
        if (cleaned.equals("taxi") || cleaned.equals("minibus") || cleaned.equals("minibus taxi")) return TAXI;
        throw new IllegalArgumentException("Unrecognized transport mode: " + raw);
    }
}
