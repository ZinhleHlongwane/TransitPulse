package transitpulse.status;

import transitpulse.model.TransportMode;

/**
 * Current status of a route (bus/train) or rank (taxi).
 * Bus/train use ON_TIME / DELAYED / CANCELLED.
 * Taxi uses LOADING / DEPARTING / FULL (taxis leave once loaded rather
 * than running to a fixed timetable).
 */
public final class StatusInfo {

    public enum Code {
        ON_TIME, DELAYED, CANCELLED,   // bus / train
        LOADING, DEPARTING, FULL       // taxi
    }

    private final String region;
    private final String name;
    private final TransportMode mode;
    private final Code code;
    private final String message;

    public StatusInfo(String region, String name, TransportMode mode, Code code, String message) {
        this.region = region;
        this.name = name;
        this.mode = mode;
        this.code = code;
        this.message = message;
    }

    public String getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public TransportMode getMode() {
        return mode;
    }

    public Code getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return name + " (" + region + ", " + mode + "): " + code + " - " + message;
    }
}
