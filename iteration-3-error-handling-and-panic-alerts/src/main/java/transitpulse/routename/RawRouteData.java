package transitpulse.routename;

import java.util.Arrays;
import java.util.List;

/**
 * Simulates a messy raw dataset the Route-name Service must clean:
 * inconsistent casing, stray whitespace, inconsistent codes, and
 * duplicate entries recorded slightly differently.
 *
 * Format per row: "region | name | code | mode"
 */
public final class RawRouteData {

    private RawRouteData() {
    }

    public static List<String> sampleRows() {
        return Arrays.asList(
                "  gauteng | rea vaya trunk t1 | rv-t1 | bus",
                "Gauteng|Rea Vaya Trunk T1|RV-T1|Bus",          // duplicate of row above, different formatting
                "Gauteng | Metrorail Central Line | MR-CL | Train",
                "gauteng|metrorail central line|mr-cl|train",   // duplicate
                " Gauteng | Bree Street Taxi Rank | BRE-01 | taxi ",
                "Gauteng|Noord Street Taxi Rank|NOO-01|Minibus",
                "Western Cape | MyCiTi T01 | MC-T01 |bus",
                "western cape|MyCiTi T01|MC-T01|BUS",           // duplicate
                "Western Cape|Cape Town Station Line|CT-SL|train",
                "Western Cape | Bellville Taxi Rank |BEL-01| taxi",
                "KwaZulu-Natal|Durban People Mover|DBN-PM|bus",
                "KwaZulu-Natal | Warwick Junction Taxi Rank | WAR-01 | minibus taxi"
        );
    }
}
