package io.github.piotr_janczukowicz.testsupport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public final class LoopbackPorts {

    private LoopbackPorts() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Returns a loopback port that nothing listens on: it is bound to learn a free number, then released. */
    public static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
