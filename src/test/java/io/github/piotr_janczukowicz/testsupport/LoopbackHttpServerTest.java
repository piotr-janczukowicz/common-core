package io.github.piotr_janczukowicz.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

@UnitTest
class LoopbackHttpServerTest {

    private static final byte[] BODY = "pong".getBytes(StandardCharsets.UTF_8);

    @Test
    void start_shouldServeHandler_whenRequestSent() throws Exception {
        try (LoopbackHttpServer server = LoopbackHttpServer.start("/ping", exchange -> {
            exchange.sendResponseHeaders(200, BODY.length);
            exchange.getResponseBody().write(BODY);
            exchange.close();
        })) {
            final HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(HttpRequest.newBuilder(server.uri()).build(), BodyHandlers.ofString());

            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(200);
                softly.assertThat(response.body()).isEqualTo("pong");
            });
        }
    }

    @Test
    void uri_shouldUseGivenPath_whenOverloadCalled() throws IOException {
        try (LoopbackHttpServer server = LoopbackHttpServer.start("/ping", HttpExchange::close)) {
            assertSoftly(softly -> {
                softly.assertThat(server.uri("/other").getPath()).isEqualTo("/other");
                softly.assertThat(server.uri().getPort()).isEqualTo(server.port());
            });
        }
    }

    @Test
    void close_shouldFreePort_whenClosed() throws Exception {
        final LoopbackHttpServer server = LoopbackHttpServer.start("/ping", HttpExchange::close);
        final HttpRequest request = HttpRequest.newBuilder(server.uri()).build();
        server.close();

        final Throwable thrown =
                catchThrowable(() -> HttpClient.newHttpClient().send(request, BodyHandlers.discarding()));

        assertThat(thrown).isInstanceOf(ConnectException.class);
    }

    @Test
    void close_shouldKeepExecutorRunning_whenExecutorSupplied() throws IOException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            LoopbackHttpServer.start("/ping", HttpExchange::close, executor).close();

            assertThat(executor.isShutdown()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void start_shouldRejectNull_whenArgumentMissing() {
        final Throwable thrown = catchThrowable(() -> LoopbackHttpServer.start(null, exchange -> {}));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("path must not be null");
    }
}
