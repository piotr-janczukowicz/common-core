package io.github.piotr_janczukowicz.http.uri;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class HttpUrisLoopbackTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "localhost",
                "LOCALHOST",
                "app.localhost",
                "127.0.0.1",
                "127.1.2.3",
                "127.255.255.255",
                "127.0.0.0",
                "[::1]",
                "[0:0:0:0:0:0:0:1]"
            })
    void isLoopbackHost_shouldBeTrue_whenHostIsLoopback(final String host) {
        assertThat(HttpUris.isLoopbackHost(host)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "api.example.test",
                "127.evil.example",
                "127.999.1.1",
                "127.0.0.256",
                "127.0.0",
                "127.0.0.1.1",
                "127.01.0.1x",
                "128.0.0.1",
                "[2001:db8::1]",
                "[not-an-address]",
                "[open",
                "closed]",
                ""
            })
    void isLoopbackHost_shouldBeFalse_whenHostIsNotLoopback(final String host) {
        assertThat(HttpUris.isLoopbackHost(host)).isFalse();
    }

    @Test
    void isLoopbackHost_shouldBeFalse_whenHostIsNull() {
        assertThat(HttpUris.isLoopbackHost(null)).isFalse();
    }
}
