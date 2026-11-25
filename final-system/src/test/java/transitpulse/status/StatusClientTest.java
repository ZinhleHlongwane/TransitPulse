package transitpulse.status;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import transitpulse.errors.InvalidRequestException;
import transitpulse.errors.MalformedResponseException;
import transitpulse.errors.RemoteServiceException;
import transitpulse.errors.ServiceUnavailableException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StatusClientTest {

    @Test
    void mapsFourHundredResponsesToInvalidRequest() throws Exception {
        withResponse(400, "{\"error\":\"missing query parameter\"}", url ->
                assertThrows(InvalidRequestException.class,
                        () -> new StatusClient(url).getStatus("Gauteng", "Route")));
    }

    @Test
    void mapsNotFoundToInvalidRequestAndPreservesStatusCode() throws Exception {
        withResponse(404, "{\"error\":\"unknown route\"}", url -> {
            InvalidRequestException error = assertThrows(InvalidRequestException.class,
                    () -> new StatusClient(url).getStatus("Gauteng", "Missing"));
            assertEquals(404, error.getStatusCode());
            assertTrue(error.getMessage().contains("unknown route"));
        });
    }

    @Test
    void mapsServerErrorsToRemoteServiceException() throws Exception {
        withResponse(503, "{\"error\":\"database unavailable\"}", url -> {
            RemoteServiceException error = assertThrows(RemoteServiceException.class,
                    () -> new StatusClient(url).getStatus("Gauteng", "Route"));
            assertEquals(503, error.getStatusCode());
        });
    }

    @Test
    void mapsConnectionFailureToServiceUnavailable() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        server.stop(0);

        assertThrows(ServiceUnavailableException.class,
                () -> new StatusClient("http://127.0.0.1:" + port)
                        .getStatus("Gauteng", "Route"));
    }

    @Test
    void mapsInvalidSuccessfulPayloadToMalformedResponse() throws Exception {
        withResponse(200, "{\"region\":\"Gauteng\",\"name\":\"Route\"}", url ->
                assertThrows(MalformedResponseException.class,
                        () -> new StatusClient(url).getStatus("Gauteng", "Route")));
    }

    private static void withResponse(int status, String body, UrlAssertion assertion) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/status", exchange -> {
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
