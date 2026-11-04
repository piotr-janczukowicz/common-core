package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

class BufferedJsonTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {"[]|false", "{}|true", "{\"a\":1}|false"})
    void isEmptyObject_shouldReturnExpected_whenValueVaries(final String input, final boolean expected) {
        final boolean empty;

        try (BufferedJson json = capture(input)) {
            empty = json.isEmptyObject();
        }

        assertThat(empty).isEqualTo(expected);
    }

    @Test
    void hasField_shouldBeFalse_whenValueIsNotObject() {
        final boolean present;

        try (BufferedJson json = capture("null")) {
            present = json.hasField("field");
        }

        assertThat(present).isFalse();
    }

    @Test
    void requiredText_shouldThrow_whenFieldMissing() {
        final Throwable thrown;

        try (BufferedJson json = capture("{\"a\":\"x\"}")) {
            thrown = catchThrowable(() -> json.requiredText("missing"));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("missing");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"a\":{}}", "{\"a\":[]}"})
    void requiredText_shouldSayWrongShape_whenFieldIsObjectOrArray(final String input) {
        final Throwable thrown;

        try (BufferedJson json = capture(input)) {
            thrown = catchThrowable(() -> json.requiredText("a"));
        }

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("must be a string, a number or a boolean");
    }

    @Test
    void requiredText_shouldSayMissing_whenFieldIsNull() {
        final Throwable thrown;

        try (BufferedJson json = capture("{\"a\":null}")) {
            thrown = catchThrowable(() -> json.requiredText("a"));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Missing required field a");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"n\":null}"})
    void requiredLong_shouldThrow_whenFieldMissingOrNull(final String input) {
        final Throwable thrown;

        try (BufferedJson json = capture(input)) {
            thrown = catchThrowable(() -> json.requiredLong("n"));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"n\":1.9}", "{\"n\":\"12\"}"})
    void requiredLong_shouldThrow_whenValueIsNotAnInteger(final String input) {
        final Throwable thrown;

        try (BufferedJson json = capture(input)) {
            thrown = catchThrowable(() -> json.requiredLong("n"));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("must be an integer");
    }

    @Test
    void requiredLong_shouldReturnValue_whenValueIsAnInteger() {
        final long value;

        try (BufferedJson json = capture("{\"n\":12}")) {
            value = json.requiredLong("n");
        }

        assertThat(value).isEqualTo(12L);
    }

    @Test
    void requiredWire_shouldReturnConstant_whenValueKnown() {
        final String converted;

        try (BufferedJson json = capture("{\"kind\":\"a\"}")) {
            converted = json.requiredWire("kind", String::toUpperCase);
        }

        assertThat(converted).isEqualTo("A");
    }

    @Test
    void requiredWire_shouldThrowDatabindException_whenConverterRejectsValue() {
        final Throwable thrown;

        try (BufferedJson json = capture("{\"kind\":\"zzz\"}")) {
            thrown = catchThrowable(() -> json.requiredWire("kind", value -> {
                throw new IllegalArgumentException("Unknown kind: " + value);
            }));
        }

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageStartingWith("Unknown kind: zzz")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiredWire_shouldThrowDatabindException_whenFieldMissing() {
        final Throwable thrown;

        try (BufferedJson json = capture("{}")) {
            thrown = catchThrowable(() -> json.requiredWire("kind", String::toUpperCase));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("kind");
    }

    @Test
    void mismatch_shouldReturnDatabindException() {
        final DatabindException mismatch;

        try (BufferedJson json = capture("{}")) {
            mismatch = json.mismatch("unexpected shape");
        }

        assertThat(mismatch).isInstanceOf(DatabindException.class).hasMessageStartingWith("unexpected shape");
    }

    @Test
    void readRequiredField_shouldThrow_whenFieldMissing() {
        final Throwable thrown;

        try (BufferedJson json = capture("{}")) {
            thrown = catchThrowable(() -> json.readRequiredField("field", String.class));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("field");
    }

    @Test
    void firstNonNull_shouldReturnHighestPriority_whenSeveralFieldsPresent() {
        final int index;

        try (BufferedJson json = capture("{\"b\":{\"x\":[1]},\"c\":1,\"a\":null}")) {
            index = json.firstNonNull(List.of("a", "b", "c"));
        }

        assertThat(index).isEqualTo(1);
    }

    @Test
    void firstNonNull_shouldReturnZero_whenTopPriorityFieldPresent() {
        final int index;

        try (BufferedJson json = capture("{\"a\":1,\"b\":2}")) {
            index = json.firstNonNull(List.of("a", "b"));
        }

        assertThat(index).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"a\":null,\"a\":1}", "{\"z\":1}", "[1]", "{\"outer\":{\"a\":1}}"})
    void firstNonNull_shouldReturnMinusOne_whenNoTopLevelNonNullField(final String input) {
        final int index;

        try (BufferedJson json = capture(input)) {
            index = json.firstNonNull(List.of("a"));
        }

        assertThat(index).isEqualTo(-1);
    }

    @Test
    void hasNonNull_shouldBeFalse_whenFieldIsJsonNull() {
        final boolean present;

        try (BufferedJson json = capture("{\"field\":null}")) {
            present = json.hasNonNull("field");
        }

        assertThat(present).isFalse();
    }

    @Test
    void readField_shouldReturnNull_whenFieldIsJsonNull() {
        final String value;

        try (BufferedJson json = capture("{\"field\":null}")) {
            value = json.readField("field", String.class);
        }

        assertThat(value).isNull();
    }

    @Test
    void readReplacingText_shouldReplaceTheField_whenItIsPresent() {
        final Tagged tagged;

        try (BufferedJson json = capture("{\"kind\":\"old\",\"n\":1}")) {
            tagged = json.readReplacingText("kind", "new", Tagged.class);
        }

        assertThat(tagged).isEqualTo(new Tagged("new", 1));
    }

    @Test
    void readReplacingText_shouldAddTheField_whenItIsAbsent() {
        final Tagged tagged;

        try (BufferedJson json = capture("{\"n\":2}")) {
            tagged = json.readReplacingText("kind", "added", Tagged.class);
        }

        assertThat(tagged).isEqualTo(new Tagged("added", 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1]", "\"text\""})
    void readReplacingText_shouldThrow_whenValueIsNotObject(final String input) {
        final Throwable thrown;

        try (BufferedJson json = capture(input)) {
            thrown = catchThrowable(() -> json.readReplacingText("a", "x", String.class));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("Expected an object");
    }

    @Test
    void readField_shouldBindGenericType_whenFieldPresent() {
        final JavaType type = MAPPER.constructType(List.class);

        final Object value;

        try (BufferedJson json = capture("{\"a\":[1,2]}")) {
            value = json.readField("a", type);
        }

        assertThat(value).isEqualTo(List.of(1, 2));
    }

    @Test
    void readRequiredField_shouldReturnValue_whenFieldPresent() {
        final String value;

        try (BufferedJson json = capture("{\"a\":\"x\"}")) {
            value = json.readRequiredField("a", String.class);
        }

        assertThat(value).isEqualTo("x");
    }

    private static BufferedJson capture(final String json) {
        try (JsonParser parser = MAPPER.createParser(json)) {
            parser.nextToken();
            return BufferedJson.capture(parser, MAPPER._deserializationContext());
        }
    }

    private record Tagged(String kind, int n) {}
}
