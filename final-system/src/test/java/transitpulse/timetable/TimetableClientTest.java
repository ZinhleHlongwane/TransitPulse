package transitpulse.timetable;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import transitpulse.errors.InvalidRequestException;
import transitpulse.errors.RemoteServiceException;
import transitpulse.errors.ServiceUnavailableException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TimetableClientTest {

    @Test
    void mapsUnknownRouteResponseToInvalidRequest() throws Exception {
        withResponse(404, "{\"error\":\"unknown route\"}", url -> {
            InvalidRequestException error = assertThrows(InvalidRequestException.class,
                    () -> new TimetableClient(url).getTimetable("Gauteng", "Missing"));
            assertEquals(404, error.getStatusCode());
        });
    }

    @Test
    void mapsServerFailureToRemoteServiceException() throws Exception {
        withResponse(500, "{\"error\":\"internal error\"}", url -> {
            RemoteServiceException error = assertThrows(RemoteServiceException.class,
                    () -> new TimetableClient(url).getTimetable("Gauteng", "Route"));
            assertEquals(500, error.getStatusCode());
        });
    }

    @Test
    void mapsSuccessfulResponseWithRequiredFields() throws Exception {
        withResponse(200, "{\"region\":\"Gauteng\",\"name\":\"Route\",\"times\":[\"06:00\",\"06:30\"],\"note\":\"Fixed timetable\"}", url -> {
            TimetableEntry entry = new TimetableClient(url).getTimetable("Gauteng", "Route");
            assertEquals("Gauteng", entry.getRegion());
            assertEquals("Route", entry.getName());
            assertEquals(2, entry.getFixedTimes().size());
        });
    }

    private static void withResponse(int status, String body, UrlAssertion assertion) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/timetable", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        try {
            assertion.accept("http://127.0.0.1:" + server.getAddress().getPort());
        } finally {
            server.stop(0);
        }
    }

    @FunctionalInterface
    private interface UrlAssertion {
        void accept(String url) throws Exception;
    }
}
