package transitpulse.alertbot;

/**
 * Simulates posting to a public social-media-style feed by printing
 * to the console. A real deployment would swap this for an actual
 * social media API client behind the same PublicAlertPublisher interface.
 */
public class ConsolePublicAlertPublisher implements PublicAlertPublisher {
    @Override
    public void postAlert(String message) {
        System.out.println("[SocialMedia @TransitPulseAlerts] " + message);
    }
}
