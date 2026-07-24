package transitpulse.model;

import java.util.Objects;

/**
 * A single cleaned route/rank record.
 *
 * For BUS/TRAIN, "name" is the route name (e.g. "Rea Vaya Trunk T1").
 * For TAXI, "name" is the rank name (e.g. "Bree Street Taxi Rank").
 */
public final class RouteEntry {

    private final String region;
    private final String name;
    private final String code;
    private final TransportMode mode;

    public RouteEntry(String region, String name, String code, TransportMode mode) {
        this.region = Objects.requireNonNull(region);
        this.name = Objects.requireNonNull(name);
        this.code = Objects.requireNonNull(code);
        this.mode = Objects.requireNonNull(mode);
    }

    public String getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public TransportMode getMode() {
        return mode;
    }

    /**
     * Identity key used for de-duplication: same region + same name (case-insensitive)
     * is considered the same route/rank regardless of formatting differences.
     */
    public String identityKey() {
        return region.toLowerCase() + "|" + name.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteEntry)) return false;
        RouteEntry other = (RouteEntry) o;
        return identityKey().equals(other.identityKey()) && mode == other.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey(), mode);
    }

    @Override
    public String toString() {
        return "RouteEntry{" +
                "region='" + region + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", mode=" + mode +
                '}';
    }
}
