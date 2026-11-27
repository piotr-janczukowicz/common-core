package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    void copyWithNullValues_shouldKeepTheOrderAndTheNulls_whenValuesHasThem() {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("z", 1);
        values.put("a", null);
        values.put("m", "x");

        final Map<String, Object> copy = MapUtils.copyWithNullValues(values);

        assertSoftly(softly -> {
            softly.assertThat(copy).isEqualTo(values).isNotSameAs(values);
            softly.assertThat(copy.keySet()).containsExactly("z", "a", "m");
            softly.assertThat(copy).containsEntry("a", null);
        });
    }

    @Test
    void copyWithNullValues_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Map<String, Object> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, Object> copy = MapUtils.copyWithNullValues(values);
        values.put("c", "d");

        assertSoftly(softly -> {
            softly.assertThat(copy).containsOnlyKeys("a");
            softly.assertThat(catchThrowable(() -> copy.put("e", "f")))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copyWithNullValues_shouldReturnNull_whenValuesIsNull() {
        final Map<String, Object> copy = MapUtils.copyWithNullValues(null);

        assertThat(copy).isNull();
    }

    @Test
    void copy_shouldReturnNull_whenValuesIsNull() {
        final Map<String, String> copy = MapUtils.copy(null);

        assertThat(copy).isNull();
    }

    @Test
    void copy_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> copy = MapUtils.copy(values);

        assertThat(copy).isEqualTo(values).isNotSameAs(values);
    }

    @Test
    void copy_shouldThrow_whenValuesHasANullElement() {
        final Map<String, String> values = new HashMap<>();
        values.put("a", null);

        final Throwable thrown = catchThrowable(() -> MapUtils.copy(values));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copyNonNull_shouldReturnAnEmptyCollection_whenValuesIsNull() {
        final Map<String, String> copy = MapUtils.copyNonNull(null);

        assertThat(copy).isEmpty();
    }

    @Test
    void copyNonNull_shouldReturnACopy_whenValuesIsGiven() {
        final Map<String, String> values = new HashMap<>(Map.of("a", "b"));

        final Map<String, String> copy = MapUtils.copyNonNull(values);

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
