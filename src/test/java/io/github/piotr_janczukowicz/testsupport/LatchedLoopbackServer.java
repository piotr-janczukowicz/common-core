package io.github.piotr_janczukowicz.testsupport;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

abstract class LatchedLoopbackServer implements AutoCloseable {

    protected final CountDownLatch released = new CountDownLatch(1);
    private final LoopbackHttpServer server;

    protected LatchedLoopbackServer(final String path) throws IOException {
        this.server = LoopbackHttpServer.start(path, this::respond);
    }

    public final URI uri() {
        return server.uri();
    }

    @Override
    public final void close() {
        released.countDown();
        server.close();
    }

    protected abstract void respond(HttpExchange exchange) throws IOException;
}
