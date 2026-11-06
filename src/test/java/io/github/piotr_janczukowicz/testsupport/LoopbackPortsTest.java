package io.github.piotr_janczukowicz.testsupport;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import org.junit.jupiter.api.Test;

@UnitTest
class LoopbackPortsTest {

    @Test
    void closedPort_shouldRefuseConnections_whenPortReturned() throws Exception {
        final int port = LoopbackPorts.closedPort();

        final Throwable thrown = catchThrowable(() -> new Socket(InetAddress.getLoopbackAddress(), port).close());

        assertSoftly(softly -> {
            softly.assertThat(port).isPositive();
            softly.assertThat(thrown).isInstanceOf(ConnectException.class);
        });
    }
}
