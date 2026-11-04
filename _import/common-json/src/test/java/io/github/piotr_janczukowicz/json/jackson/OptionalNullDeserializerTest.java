package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonDecodeException;
import io.github.piotr_janczukowicz.json.JsonTestCodec;
import io.github.piotr_janczukowicz.json.JsonType;
import io.github.piotr_janczukowicz.json.JsonTypes;
import io.github.piotr_janczukowicz.json.OptionalNull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;

class OptionalNullDeserializerTest {

    private static final JsonCodec CODEC = JsonTestCodec.defaults();
    private static final JsonType<OptionalNull<String>> NULLABLE_STRING =
            JsonTypes.of(OptionalNull.class, String.class);

    @Test
    void decode_shouldWrapValue_whenRootOptionalNullPresent() {
        final byte[] json = "\"octocat\"".getBytes(StandardCharsets.UTF_8);

        final OptionalNull<String> decoded = CODEC.decode(json, NULLABLE_STRING);

        assertThat(decoded.value()).isEqualTo("octocat");
    }

    @Test
    void decode_shouldWrapJsonNull_whenRootOptionalNullNull() {
        final byte[] json = "null".getBytes(StandardCharsets.UTF_8);

        final OptionalNull<String> decoded = CODEC.decode(json, NULLABLE_STRING);

        assertThat(decoded.value()).isNull();
    }

    @Test
    void decode_shouldWrapValue_whenStringFieldPresent() {
        final byte[] json = "{\"field\":\"octocat\"}".getBytes(StandardCharsets.UTF_8);

        final StringPayload payload = CODEC.decode(json, StringPayload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isEqualTo("octocat");
        });
    }

    @Test
    void decode_shouldWrapJsonNull_whenStringFieldNull() {
        final byte[] json = "{\"field\":null}".getBytes(StandardCharsets.UTF_8);

        final StringPayload payload = CODEC.decode(json, StringPayload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isNull();
        });
    }

    @Test
    void decode_shouldLeaveWrapperNull_whenStringFieldAbsent() {
        final byte[] json = "{}".getBytes(StandardCharsets.UTF_8);

        final StringPayload payload = CODEC.decode(json, StringPayload.class);

        assertThat(payload.field()).isNull();
    }

    @Test
    void decode_shouldFail_whenWrappedEnumValueIsUnknown() {
        final byte[] json = "{\"field\":\"not-a-color\"}".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, EnumPayload.class));

        assertThat(thrown).isInstanceOf(JsonDecodeException.class).hasMessageContaining("OptionalNull");
    }

    @Test
    void decode_shouldWrapEnum_whenWrappedEnumValueIsKnown() {
        final byte[] json = "{\"field\":\"red\"}".getBytes(StandardCharsets.UTF_8);

        final EnumPayload decoded = CODEC.decode(json, EnumPayload.class);

        assertThat(decoded.field().value()).isEqualTo(Color.RED);
    }

    @Test
    void decode_shouldWrapList_whenListFieldPresent() {
        final byte[] json = "{\"field\":[\"a\",\"b\"]}".getBytes(StandardCharsets.UTF_8);

        final ListPayload payload = CODEC.decode(json, ListPayload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).containsExactly("a", "b");
        });
    }

    @Test
    void decode_shouldWrapJsonNull_whenListFieldNull() {
        final byte[] json = "{\"field\":null}".getBytes(StandardCharsets.UTF_8);

        final ListPayload payload = CODEC.decode(json, ListPayload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isNull();
        });
    }

    @Test
    void decode_shouldLeaveWrapperNull_whenListFieldAbsent() {
        final byte[] json = "{}".getBytes(StandardCharsets.UTF_8);

        final ListPayload payload = CODEC.decode(json, ListPayload.class);

        assertThat(payload.field()).isNull();
    }

    @Test
    void decode_shouldFail_whenWrappedValueReadsAsNull() {
        final byte[] json = "{\"field\":\"gone\"}".getBytes(StandardCharsets.UTF_8);

        final Throwable thrown = catchThrowable(() -> CODEC.decode(json, NullingPayload.class));

        assertThat(thrown).isInstanceOf(JsonDecodeException.class).hasMessageContaining("NullingPayload");
    }

    @Test
    void deserialize_shouldThrow_whenNotContextualized() {
        final OptionalNullDeserializer deserializer = new OptionalNullDeserializer();

        final Throwable thrown = catchThrowable(() -> deserializer.deserialize(null, null));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("contextualized");
    }

    @Test
    void createContextual_shouldThrow_whenNoTypeIsKnown() {
        final DeserializationContext context = mock(DeserializationContext.class);
        final OptionalNullDeserializer deserializer = new OptionalNullDeserializer();

        final Throwable thrown = catchThrowable(() -> deserializer.createContextual(context, null));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StringPayload(@Nullable OptionalNull<String> field) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ListPayload(@Nullable OptionalNull<List<String>> field) {}

    private enum Color {
        RED("red");

        private final String wire;

        Color(final String wire) {
            this.wire = wire;
        }

        @JsonValue
        String wire() {
            return wire;
        }

        @JsonCreator
        static Color fromWire(final String value) {
            if (RED.wire.equals(value)) {
                return RED;
            }
            throw new IllegalArgumentException("unknown color " + value);
        }
    }

    private record EnumPayload(@Nullable OptionalNull<Color> field) {}

    private enum Nulling {
        PRESENT;

        @JsonCreator
        static @Nullable Nulling fromWire(final String value) {
            return "present".equals(value) ? PRESENT : null;
        }
    }

    private record NullingPayload(@Nullable OptionalNull<Nulling> field) {}
}
