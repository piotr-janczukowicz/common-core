package io.github.piotr_janczukowicz.testsupport;

import static io.github.piotr_janczukowicz.testsupport.Preconditions.requireNonNull;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A JDK {@link HttpServer} bound to the loopback address on a free port, serving one handler at one path.
 *
 * <p>With {@link #start(String, HttpHandler)} the server owns a cached daemon thread pool and shuts it down on
 * {@link #close()}. With {@link #start(String, HttpHandler, Executor)} the caller keeps ownership of the executor.
 */
public final class LoopbackHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final String path;
    private final Runnable cleanup;

    private LoopbackHttpServer(final HttpServer server, final String path, final Runnable cleanup) {
        this.server = server;
        this.path = path;
        this.cleanup = cleanup;
    }

    public static LoopbackHttpServer start(final String path, final HttpHandler handler) throws IOException {
        requireNonNull(path, "path");
        requireNonNull(handler, "handler");
        final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
            final Thread thread = new Thread(runnable, "loopback-http-server");
            thread.setDaemon(true);
            return thread;
        });
        return create(path, handler, executor, executor::shutdownNow);
    }

    public static LoopbackHttpServer start(final String path, final HttpHandler handler, final Executor executor)
            throws IOException {
        requireNonNull(path, "path");
        requireNonNull(handler, "handler");
        requireNonNull(executor, "executor");
        return create(path, handler, executor, () -> {});
    }

    public URI uri() {
        return uri(path);
    }

    public URI uri(final String requestPath) {
        requireNonNull(requestPath, "requestPath");
        return URI.create("http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + port() + requestPath);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        cleanup.run();
    }

    private static LoopbackHttpServer create(
            final String path, final HttpHandler handler, final Executor executor, final Runnable cleanup)
            throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
        server.createContext(path, handler);
        server.start();
        return new LoopbackHttpServer(server, path, cleanup);
    }
}
