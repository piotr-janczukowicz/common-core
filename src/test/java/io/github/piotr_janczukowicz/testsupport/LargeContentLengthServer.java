package io.github.piotr_janczukowicz.testsupport;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public final class LargeContentLengthServer extends LatchedLoopbackServer {

    private static final long ANNOUNCED_LENGTH = 10L * 1024 * 1024;

    public LargeContentLengthServer() throws IOException {
        super("/large");
    }

    @Override
    protected void respond(final HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, ANNOUNCED_LENGTH);
        try (exchange) {
            released.await();
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
