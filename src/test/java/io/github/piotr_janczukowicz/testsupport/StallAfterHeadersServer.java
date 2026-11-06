package io.github.piotr_janczukowicz.testsupport;

import static io.github.piotr_janczukowicz.testsupport.Preconditions.requireNonNull;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class StallAfterHeadersServer extends LatchedLoopbackServer {

    private final Duration stall;

    public StallAfterHeadersServer(final Duration stall) throws IOException {
        super("/stall");
        this.stall = requireNonNull(stall, "stall");
    }

    @Override
    protected void respond(final HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, 2);
        try (exchange) {
            released.await(stall.toMillis(), TimeUnit.MILLISECONDS);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(new byte[] {1, 2});
            }
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
