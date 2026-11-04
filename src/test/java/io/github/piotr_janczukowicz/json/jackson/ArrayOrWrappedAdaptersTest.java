package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

class ArrayOrWrappedAdaptersTest {

    private static final JsonMapper MAPPER = mapper();

    @Test
    void deserialize_shouldBuildFromBareArray_whenValueIsArray() {
        final Names names = MAPPER.readValue("[\"a\",\"b\"]", Names.class);

        assertThat(names).isEqualTo(new Names(List.of("a", "b"), null));
    }

    @Test
    void deserialize_shouldBuildFromWrappedList_whenValueIsObject() {
        final Names names = MAPPER.readValue("{\"items\":[\"a\"]}", Names.class);

        assertThat(names).isEqualTo(new Names(null, List.of("a")));
    }

    @Test
    void serialize_shouldWriteBareArray_whenArraySet() {
        final Names names = new Names(List.of("a"), null);

        final byte[] json = MAPPER.writeValueAsBytes(names);
        final Object written = MAPPER.readValue(json, Object.class);

        assertThat(written).isEqualTo(List.of("a"));
    }

    @Test
    void serialize_shouldWriteWrappedObject_whenWrappedSet() {
        final Names names = new Names(null, List.of("a"));

        final byte[] json = MAPPER.writeValueAsBytes(names);
        final Object written = MAPPER.readValue(json, Object.class);

        assertThat(written).isEqualTo(Map.of("items", List.of("a")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"items\":null}", "5", "\"x\""})
    void deserialize_shouldExplainExpectedShape_whenNeitherArrayNorWrappedList(final String json) {
        final Throwable thrown = catchThrowable(() -> MAPPER.readValue(json, Names.class));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("array or an object with");
    }

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws Exception {
        final Constructor<ArrayOrWrappedAdapters> constructor = ArrayOrWrappedAdapters.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    private static JsonMapper mapper() {
        final SimpleModule module = new SimpleModule();
        ArrayOrWrappedAdapters.register(
                module, Names.class, "items", Names::array, Names::wrapped, Names::ofArray, Names::ofWrapped);
        return JsonMapper.builder().addModule(module).build();
    }

    private record Names(
            @Nullable List<String> array, @Nullable List<String> wrapped) {

        static Names ofArray(final List<String> values) {
            return new Names(values, null);
        }

        static Names ofWrapped(final List<String> values) {
            return new Names(null, values);
        }
    }
}
