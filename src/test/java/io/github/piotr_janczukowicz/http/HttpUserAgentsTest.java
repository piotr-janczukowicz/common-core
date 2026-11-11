package io.github.piotr_janczukowicz.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class HttpUserAgentsTest {

    @ParameterizedTest
    @ValueSource(strings = {"my-app/1.0", "my-app/1.0 (+https://example.test/about)", "a"})
    void requireValid_shouldReturnTheValue_whenItIsVisibleAscii(final String userAgent) {
        assertThat(HttpUserAgents.requireValid(userAgent)).isEqualTo(userAgent);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "app\r\nX-Evil: 1", "app\tname", "app\u007f", "café/1.0", "\u0000"})
    void requireValid_shouldRefuse_whenItIsBlankOrHoldsAnythingButVisibleAscii(final String userAgent) {
        final Throwable thrown = catchThrowable(() -> HttpUserAgents.requireValid(userAgent));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireValid_shouldRefuse_whenNull() {
        final Throwable thrown = catchThrowable(() -> HttpUserAgents.requireValid(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final var constructor = HttpUserAgents.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
