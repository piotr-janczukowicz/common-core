package io.github.piotr_janczukowicz.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OptionalNullTest {

    private static final JsonCodec CODEC = JsonTestCodec.defaults();

    @Test
    void of_shouldReturnWrappedValue_whenNonNull() {
        final OptionalNull<String> wrapper = OptionalNull.of("octocat");

        assertThat(wrapper.value()).isEqualTo("octocat");
    }

    @Test
    void of_shouldThrow_whenNull() {
        final Throwable thrown = catchThrowable(() -> OptionalNull.of(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void ofNull_shouldReturnNullValue() {
        final OptionalNull<String> wrapper = OptionalNull.ofNull();

        assertThat(wrapper.value()).isNull();
    }

    @Test
    void equals_shouldReturnTrue_whenSameInstance() {
        final OptionalNull<String> wrapper = OptionalNull.of("octocat");

        final boolean equal = wrapper.equals(wrapper);

        assertThat(equal).isTrue();
    }

    @Test
    void equals_shouldReturnTrue_whenEqualValues() {
        final OptionalNull<String> first = OptionalNull.of("octocat");
        final OptionalNull<String> second = OptionalNull.of("octocat");

        final boolean equal = first.equals(second);

        assertThat(equal).isTrue();
    }

    @Test
    void equals_shouldReturnTrue_whenBothJsonNull() {
        final OptionalNull<String> first = OptionalNull.ofNull();
        final OptionalNull<String> second = OptionalNull.ofNull();

        final boolean equal = first.equals(second);

        assertThat(equal).isTrue();
    }

    @Test
    void equals_shouldReturnFalse_whenValuesDiffer() {
        final OptionalNull<String> first = OptionalNull.of("octocat");
        final OptionalNull<String> second = OptionalNull.of("mona");

        final boolean equal = first.equals(second);

        assertThat(equal).isFalse();
    }

    @Test
    void equals_shouldReturnFalse_whenOtherType() {
        final OptionalNull<String> wrapper = OptionalNull.of("octocat");
        final Object other = "octocat";

        final boolean equal = wrapper.equals(other);

        assertThat(equal).isFalse();
    }

    @Test
    void hashCode_shouldMatch_whenEqualValues() {
        final OptionalNull<String> first = OptionalNull.of("octocat");
        final OptionalNull<String> second = OptionalNull.of("octocat");

        final int hash = first.hashCode();

        assertThat(hash).isEqualTo(second.hashCode());
    }

    @Test
    void hashCode_shouldMatch_whenBothJsonNull() {
        final OptionalNull<String> first = OptionalNull.ofNull();
        final OptionalNull<String> second = OptionalNull.ofNull();

        final int hash = first.hashCode();

        assertThat(hash).isEqualTo(second.hashCode());
    }

    @ParameterizedTest
    @MethodSource("encodeCases")
    void encode_shouldWriteFields_whenFieldState(final Payload body, final Map<String, Object> expectedFields) {
        final byte[] json = CODEC.encode(body);
        final Map<?, ?> fields = CODEC.decode(json, Map.class);

        assertThat(fields).isEqualTo(expectedFields);
    }

    @Test
    void decode_shouldWrapValue_whenFieldPresent() {
        final byte[] json = "{\"field\":\"octocat\"}".getBytes(StandardCharsets.UTF_8);

        final Payload payload = CODEC.decode(json, Payload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isEqualTo("octocat");
        });
    }

    @Test
    void decode_shouldWrapJsonNull_whenFieldNull() {
        final byte[] json = "{\"field\":null}".getBytes(StandardCharsets.UTF_8);

        final Payload payload = CODEC.decode(json, Payload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isNull();
        });
    }

    @Test
    void decode_shouldLeaveWrapperNull_whenFieldAbsent() {
        final byte[] json = "{}".getBytes(StandardCharsets.UTF_8);

        final Payload payload = CODEC.decode(json, Payload.class);

        assertThat(payload.field()).isNull();
    }

    @Test
    void decode_shouldWrapEnum_whenTypedOptionalNull() {
        final byte[] json = "{\"field\":\"ACTIVE\"}".getBytes(StandardCharsets.UTF_8);

        final EnumPayload payload = CODEC.decode(json, EnumPayload.class);

        assertSoftly(softly -> {
            softly.assertThat(payload.field()).isNotNull();
            softly.assertThat(payload.field().value()).isEqualTo(Sample.ACTIVE);
        });
    }

    @Test
    void toString_shouldShowValue_whenValuePresent() {
        final OptionalNull<String> wrapper = OptionalNull.of("abc");

        final String text = wrapper.toString();

        assertThat(text).isEqualTo("OptionalNull[abc]");
    }

    @Test
    void toString_shouldShowNull_whenExplicitNull() {
        final OptionalNull<String> wrapper = OptionalNull.ofNull();

        final String text = wrapper.toString();

        assertThat(text).isEqualTo("OptionalNull[null]");
    }

    private static Stream<Arguments> encodeCases() {
        return Stream.of(
                Arguments.of(new Payload(OptionalNull.of("octocat")), Map.of("field", "octocat")),
                Arguments.of(new Payload(OptionalNull.ofNull()), Collections.singletonMap("field", null)),
                Arguments.of(new Payload(null), Map.of()));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Payload(@Nullable OptionalNull<String> field) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EnumPayload(@Nullable OptionalNull<Sample> field) {}

    enum Sample {
        ACTIVE
    }
}
