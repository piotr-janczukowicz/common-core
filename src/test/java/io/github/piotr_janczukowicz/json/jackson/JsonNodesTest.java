package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class JsonNodesTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        final Constructor<JsonNodes> constructor = JsonNodes.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void textOrNull_shouldReturnText_whenFieldIsAString() {
        final JsonNode node = MAPPER.readTree("{\"message\":\"boom\"}");

        final String text = JsonNodes.textOrNull(node, "message");

        assertThat(text).isEqualTo("boom");
    }

    @Test
    void textOrNull_shouldReturnNull_whenFieldIsMissing() {
        final JsonNode node = MAPPER.readTree("{}");

        final String text = JsonNodes.textOrNull(node, "message");

        assertThat(text).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"message\":12}", "{\"message\":null}", "{\"message\":[\"a\"]}"})
    void textOrNull_shouldReturnNull_whenFieldIsNotAString(final String json) {
        final JsonNode node = MAPPER.readTree(json);

        final String text = JsonNodes.textOrNull(node, "message");

        assertThat(text).isNull();
    }
}
