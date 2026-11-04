package io.github.piotr_janczukowicz.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TestCodecTest {

    private static final JsonCodec CODEC = JsonTestCodec.defaults();

    @Test
    void encode_shouldThrow_whenValueCannotBeWritten() {
        final Unwritable value = new Unwritable();

        final Throwable thrown = catchThrowable(() -> CODEC.encode(value));

        assertThat(thrown).isInstanceOf(JsonEncodeException.class).hasMessageContaining(Unwritable.class.getName());
    }

    @Test
    void decode_shouldThrow_whenJsonIsMalformed() {
        final byte[] json = "{".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, Object.class));

        assertThat(thrown).isInstanceOf(JsonDecodeException.class);
    }

    @Test
    void decode_shouldThrow_whenJsonTypeMismatches() {
        final JsonType<List<String>> type = JsonTypes.of(List.class, String.class);
        final byte[] json = "{}".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, type));

        assertThat(thrown).isInstanceOf(JsonDecodeException.class);
    }

    @Test
    void decode_shouldThrow_whenBodyIsNull() {
        final byte[] json = "null".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, Object.class));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decode_shouldThrow_whenGenericBodyIsNull() {
        final JsonType<List<String>> type = JsonTypes.of(List.class, String.class);
        final byte[] json = "null".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, type));

        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    public static final class Unwritable {

        public String getBoom() {
            throw new IllegalStateException("no value");
        }
    }
}
