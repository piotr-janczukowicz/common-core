package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(MapUtils.class);
    }

    @Test
    void copyOf_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> copy = MapUtils.copyOf(values, "values");

        assertSoftly(softly -> {
            softly.assertThat(copy).isEqualTo(values).isNotSameAs(values);
            softly.assertThat(catchThrowable(copy::clear)).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copyOf_shouldThrowWithTheName_whenValuesIsNull() {
        final Throwable thrown = catchThrowable(() -> MapUtils.copyOf(null, "labels"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("labels");
    }

    @Test
    void copyOrNull_shouldReturnNull_whenValuesIsNull() {
        final Map<String, String> copy = MapUtils.copyOrNull(null);

        assertThat(copy).isNull();
    }

    @Test
    void copyOrNull_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> copy = MapUtils.copyOrNull(values);

        assertThat(copy).isEqualTo(values).isNotSameAs(values);
    }

    @Test
    void copyOrNull_shouldThrow_whenValuesHasANullElement() {
        final Map<String, String> values = new HashMap<>();
        values.put("a", null);

        final Throwable thrown = catchThrowable(() -> MapUtils.copyOrNull(values));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copyOrEmpty_shouldReturnAnEmptyCollection_whenValuesIsNull() {
        final Map<String, String> copy = MapUtils.copyOrEmpty(null);

        assertThat(copy).isEmpty();
    }

    @Test
    void copyOrEmpty_shouldReturnACopy_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> copy = MapUtils.copyOrEmpty(values);

        assertThat(copy).isEqualTo(values).isNotSameAs(values);
    }

    @Test
    void orEmpty_shouldReturnTheSameInstance_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> result = MapUtils.orEmpty(values);

        assertThat(result).isSameAs(values);
    }

    @Test
    void orEmpty_shouldReturnAnEmptyCollection_whenValuesIsNull() {
        final Map<String, String> result = MapUtils.orEmpty(null);

        assertThat(result).isEmpty();
    }
}
