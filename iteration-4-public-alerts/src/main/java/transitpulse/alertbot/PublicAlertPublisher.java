package transitpulse.alertbot;

/**
 * Pluggable target for public-facing alerts. A real implementation might
 * post to Twitter/X, a Slack channel, or a public webhook; swapping the
 * implementation never requires changing AlertBot's logic.
 */
public interface PublicAlertPublisher {
    void postAlert(String message);
}
