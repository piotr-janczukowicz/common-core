package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class JsonTokensTest {

    private static final JsonParser PARSER = mock(JsonParser.class);

    @Test
    void requireToken_shouldReturn_whenTokenMatches() {
        final Throwable thrown = catchThrowable(() ->
                JsonTokens.requireToken(PARSER, JsonToken.VALUE_STRING, JsonToken.VALUE_STRING, "must be a string"));

        assertThat(thrown).isNull();
    }

    @Test
    void requireToken_shouldThrow_whenTokenDiffers() {
        final Throwable thrown = catchThrowable(() -> JsonTokens.requireToken(
                PARSER, JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_STRING, "must be a string"));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageStartingWith("must be a string");
    }

    @Test
    void requireToken_shouldThrow_whenTokenIsNull() {
        final Throwable thrown =
                catchThrowable(() -> JsonTokens.requireToken(PARSER, null, JsonToken.END_ARRAY, "must end the array"));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageStartingWith("must end the array");
    }

    @Test
    void readUnlessEmptyObject_shouldReturnNull_whenObjectIsEmpty() {
        final JsonMapper mapper = userMapper();

        final Probe probe = mapper.readValue("{}", Probe.class);

        assertThat(probe.user()).isNull();
    }

    @Test
    void readUnlessEmptyObject_shouldBind_whenObjectHasProperties() {
        final JsonMapper mapper = userMapper();

        final Probe probe = mapper.readValue("{\"login\":\"a\",\"id\":3}", Probe.class);

        assertThat(probe.user()).isEqualTo(new User("a", 3));
    }

    @Test
    void readUnlessEmptyObject_shouldThrowDatabindException_whenValueIsArray() {
        final JsonMapper mapper = userMapper();

        final Throwable thrown = catchThrowable(() -> mapper.readValue("[]", Probe.class));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @Test
    void readUnlessEmptyObject_shouldFailLikeDirectBind_whenObjectMalformed() {
        final String json = "{\"login\":\"a\",\"id\":\"x\"}";
        final JsonMapper plain = JsonMapper.builder().build();
        final JsonMapper mapper = userMapper();

        final Throwable direct = catchThrowable(() -> plain.readValue(json, User.class));
        final Throwable thrown = catchThrowable(() -> mapper.readValue(json, Probe.class));

        assertThat(thrown).isInstanceOf(direct.getClass()).hasMessage(direct.getMessage());
    }

    @Test
    void constructor_shouldRejectInstantiation() throws Exception {
        final Constructor<JsonTokens> constructor = JsonTokens.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static JsonMapper userMapper() {
        return JsonMapper.builder()
                .addModule(new SimpleModule().addDeserializer(Probe.class, new EmptyAsNull()))
                .build();
    }

    record User(String login, int id) {}

    record Probe(User user) {}

    private static final class EmptyAsNull extends ValueDeserializer<Probe> {
        @Override
        public Probe deserialize(final JsonParser parser, final DeserializationContext context) {
            return new Probe(JsonTokens.readUnlessEmptyObject(parser, context, User.class));
        }
    }
}
