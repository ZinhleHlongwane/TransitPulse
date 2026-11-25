package transitpulse.timetable;

import java.util.List;

/**
 * A timetable/departure-info response for a route or rank.
 *
 * For bus/train: fixedTimes holds a list of scheduled departure times (e.g. "06:00", "06:30").
 * For taxi: fixedTimes is empty and note explains that taxis depart once loaded.
 */
public final class TimetableEntry {
    private final String region;
    private final String name;
    private final List<String> fixedTimes; // empty for taxi
    private final String note;

    public TimetableEntry(String region, String name, List<String> fixedTimes, String note) {
        this.region = region;
        this.name = name;
        this.fixedTimes = fixedTimes;
        this.note = note;
    }

    public String getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public List<String> getFixedTimes() {
        return fixedTimes;
    }

    public String getNote() {
        return note;
    }
}
