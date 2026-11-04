package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class ScalarUnionReadersTest {

    private static final ScalarUnionReaders.Factories<String> REQUIRED = new ScalarUnionReaders.Factories<>(
            value -> "text:" + value,
            value -> "number:" + value,
            null,
            values -> "texts:" + values,
            false,
            "must be scalar union");

    private static final ScalarUnionReaders.Factories<String> OPTIONAL = new ScalarUnionReaders.Factories<>(
            value -> "text:" + value,
            value -> "number:" + value,
            flag -> "flag:" + flag,
            values -> "texts:" + values,
            true,
            "must be scalar union");

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws Exception {
        final Constructor<ScalarUnionReaders> constructor = ScalarUnionReaders.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void read_shouldReturnText_whenJsonIsString() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(parser.getString()).thenReturn("bug");

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("text:bug");
    }

    @Test
    void read_shouldReturnNumber_whenJsonIsInteger() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_INT);
        when(parser.getDecimalValue()).thenReturn(BigDecimal.valueOf(2));

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("number:2");
    }

    @Test
    void read_shouldReturnNumber_whenJsonIsFloat() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(parser.getDecimalValue()).thenReturn(new BigDecimal("1.5"));

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("number:1.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E+999999999", "1E-999999999", "1E+1001", "1E-1001"})
    void read_shouldThrow_whenNumberExponentIsOutOfRange(final String number) {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(parser.getDecimalValue()).thenReturn(new BigDecimal(number));
        final DeserializationContext context = mock(DeserializationContext.class);

        final Throwable thrown = catchThrowable(() -> ScalarUnionReaders.read(parser, context, OPTIONAL));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("exponent is out of range");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E+1000", "1E-1000"})
    void read_shouldReturnNumber_whenNumberExponentIsAtTheLimit(final String number) {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(parser.getDecimalValue()).thenReturn(new BigDecimal(number));

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).startsWith("number:");
    }

    @Test
    void read_shouldKeepEveryDigit_whenNumberExceedsDoublePrecision() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NUMBER_FLOAT);
        when(parser.getDecimalValue()).thenReturn(new BigDecimal("12345678901234567890.123456789"));

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("number:12345678901234567890.123456789");
    }

    @Test
    void read_shouldReturnFlag_whenJsonIsFalse() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_FALSE);
        when(parser.getBooleanValue()).thenReturn(false);

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("flag:false");
    }

    @Test
    void read_shouldReturnFlag_whenJsonIsTrue() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_TRUE);
        when(parser.getBooleanValue()).thenReturn(true);

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isEqualTo("flag:true");
    }

    @Test
    @SuppressWarnings("unchecked")
    void read_shouldReturnTexts_whenJsonIsArray() {
        final JsonParser parser = mock(JsonParser.class);
        final DeserializationContext context = mock(DeserializationContext.class);
        when(parser.currentToken()).thenReturn(JsonToken.START_ARRAY);
        when(context.readValue(eq(parser), any(TypeReference.class))).thenReturn(List.of("a"));

        final String decoded = ScalarUnionReaders.read(parser, context, OPTIONAL);

        assertThat(decoded).isEqualTo("texts:[a]");
    }

    @Test
    void read_shouldReturnNull_whenJsonIsNullAndNullAccepted() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

        final String decoded = ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL);

        assertThat(decoded).isNull();
    }

    @Test
    void readRequired_shouldThrow_whenJsonIsNullAndNullAccepted() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

        final Throwable thrown = catchThrowable(
                () -> ScalarUnionReaders.readRequired(parser, mock(DeserializationContext.class), OPTIONAL));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("must be scalar union");
    }

    @Test
    void readRequired_shouldThrow_whenJsonIsNullAndNullRejected() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

        final Throwable thrown = catchThrowable(
                () -> ScalarUnionReaders.readRequired(parser, mock(DeserializationContext.class), REQUIRED));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("must be scalar union");
    }

    @Test
    void readRequired_shouldThrow_whenJsonIsBooleanAndFlagRejected() {
        final JsonParser parser = mock(JsonParser.class);
        when(parser.currentToken()).thenReturn(JsonToken.VALUE_FALSE);

        final Throwable thrown = catchThrowable(
                () -> ScalarUnionReaders.readRequired(parser, mock(DeserializationContext.class), REQUIRED));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("must be scalar union");
    }

    @Test
    void read_shouldThrow_whenJsonIsObject() {
        final JsonMapper mapper = JsonMapper.builder().build();
        final Throwable thrown;

        try (JsonParser parser = mapper.createParser("{}")) {
            parser.nextToken();

            thrown =
                    catchThrowable(() -> ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL));
        }

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("must be scalar union");
    }

    @Test
    void readRequired_shouldReturnValue_whenJsonIsString() {
        final JsonMapper mapper = JsonMapper.builder().build();
        final String value;

        try (JsonParser parser = mapper.createParser("\"text\"")) {
            parser.nextToken();

            value = ScalarUnionReaders.readRequired(parser, mock(DeserializationContext.class), REQUIRED);
        }

        assertThat(value).isEqualTo("text:text");
    }

    @Test
    void read_shouldThrow_whenRealParserReadsHugeExponent() {
        final JsonMapper mapper = JsonMapper.builder().build();
        final Throwable thrown;

        try (JsonParser parser = mapper.createParser("1e999999999")) {
            parser.nextToken();

            thrown =
                    catchThrowable(() -> ScalarUnionReaders.read(parser, mock(DeserializationContext.class), OPTIONAL));
        }

        assertThat(thrown)
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("number exponent is out of range");
    }

    @Test
    void read_shouldReturnTexts_whenMapperReadsStringArray() {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(Union.class, new StdDeserializer<>(Union.class) {
            @Override
            public Union deserialize(final JsonParser parser, final DeserializationContext context) {
                return new Union(ScalarUnionReaders.read(parser, context, OPTIONAL));
            }
        });
        final JsonMapper mapper = JsonMapper.builder().addModule(module).build();

        final Union decoded = mapper.readValue("[\"a\",\"b\"]", Union.class);

        assertThat(decoded.text()).isEqualTo("texts:[a, b]");
    }

    private record Union(String text) {}
}
