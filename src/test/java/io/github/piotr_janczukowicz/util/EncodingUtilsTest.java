package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EncodingUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(EncodingUtils.class);
    }

    @Test
    void decodeBase64_shouldReturnDecodedBytes_whenValid() {
        final String value = "aGVsbG8=";
        final String message = "not valid base64";

        final byte[] result = EncodingUtils.decodeBase64(value, message);

        assertThat(result).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void decodeBase64_shouldThrowWithoutCause_whenInvalid() {
        final String value = "not*base64";
        final String message = "not valid base64";

        final Throwable thrown = catchThrowable(() -> EncodingUtils.decodeBase64(value, message));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message)
                .hasNoCause();
    }

    @Test
    void decodeBase64_shouldThrow_whenNull() {
        final String value = null;
        final String message = "not valid base64";

        final Throwable thrown = catchThrowable(() -> EncodingUtils.decodeBase64(value, message));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("value must not be null");
    }

    @Test
    void decodeBase64_shouldThrow_whenMessageNull() {
        final String value = "aGVsbG8=";
        final String message = null;

        final Throwable thrown = catchThrowable(() -> EncodingUtils.decodeBase64(value, message));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("message must not be null");
    }

    @Test
    void decodeBase64_shouldThrow_whenMessageBlank() {
        final String value = "aGVsbG8=";
        final String message = " ";

        final Throwable thrown = catchThrowable(() -> EncodingUtils.decodeBase64(value, message));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("message must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"_-8=", "aGVs\r\nbG8="})
    void decodeBase64_shouldThrow_whenUrlSafeOrMimeInput(final String value) {
        final String message = "not valid base64";

        final Throwable thrown = catchThrowable(() -> EncodingUtils.decodeBase64(value, message));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }
}
