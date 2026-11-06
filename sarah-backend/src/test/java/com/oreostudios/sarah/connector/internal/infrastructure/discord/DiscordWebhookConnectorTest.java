package com.oreostudios.sarah.connector.internal.infrastructure.discord; // Same package as the unit under test.

import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext; // Operation context.
import com.oreostudios.sarah.connector.internal.infrastructure.OutboundHttpGuard; // SSRF guard (mocked).
import com.sun.net.httpserver.HttpServer; // Embedded HTTP server (JDK).
import org.junit.jupiter.api.AfterEach; // Teardown.
import org.junit.jupiter.api.BeforeEach; // Setup.
import org.junit.jupiter.api.Test; // JUnit 5.
import org.springframework.web.client.RestClient; // HTTP client.

import java.io.IOException; // Server IO.
import java.net.InetSocketAddress; // Bind address.
import java.net.URI; // Request URI.
import java.nio.charset.StandardCharsets; // UTF-8.

import static org.assertj.core.api.Assertions.assertThat; // Fluent assertions.
import static org.mockito.ArgumentMatchers.anyString; // Argument matcher.
import static org.mockito.Mockito.mock; // Mock factory.
import static org.mockito.Mockito.when; // Stubbing.

/**
 * Exercises the real Discord publish path (HTTP POST + response parsing) against a local embedded
 * HTTP server. The SSRF guard is mocked to return the test URI so the loopback check does not
 * block the test, while the connector's real logic runs.
 */
class DiscordWebhookConnectorTest {

    private HttpServer server; // Embedded server.
    private volatile String capturedBody; // Request body captured by the handler.
    private URI baseUri; // Base webhook URI.

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook", exchange -> {
            capturedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = "{\"id\":\"999\",\"channel_id\":\"555\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/webhook");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void publishesAndParsesMessageId() {
        OutboundHttpGuard guard = mock(OutboundHttpGuard.class);
        when(guard.validate(anyString())).thenReturn(baseUri);

        DiscordWebhookConnector connector = new DiscordWebhookConnector(RestClient.builder(), guard);
        ConnectorContext ctx = new ConnectorContext("test-channel", null, baseUri.toString());
        PublishResult result = connector.publish(ctx, new PublishCommand("Hello from Sarah", null, "idem-1", "en"));

        assertThat(result.success()).isTrue();
        assertThat(result.externalId()).isEqualTo("999");
        assertThat(capturedBody).contains("Hello from Sarah");
    }
}
