package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SetUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(SetUtils.class);
    }

    @Test
    void copyOf_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Set<String> values = new HashSet<>(Set.of("a", "b"));

        final Set<String> copy = SetUtils.copyOf(values, "values");

        assertSoftly(softly -> {
            softly.assertThat(copy).isEqualTo(values).isNotSameAs(values);
            softly.assertThat(catchThrowable(copy::clear)).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copyOf_shouldThrowWithTheName_whenValuesIsNull() {
        final Throwable thrown = catchThrowable(() -> SetUtils.copyOf(null, "labels"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("labels");
    }

    @Test
    void copyOrNull_shouldReturnNull_whenValuesIsNull() {
        final Set<String> copy = SetUtils.copyOrNull(null);

        assertThat(copy).isNull();
    }

    @Test
    void copyOrNull_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final Set<String> values = new HashSet<>(Set.of("a", "b"));

        final Set<String> copy = SetUtils.copyOrNull(values);

        assertThat(copy).isEqualTo(values).isNotSameAs(values);
    }

    @Test
    void copyOrNull_shouldThrow_whenValuesHasANullElement() {
        final Set<String> values = new HashSet<>(Arrays.asList("a", null));

        final Throwable thrown = catchThrowable(() -> SetUtils.copyOrNull(values));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copyOrEmpty_shouldReturnAnEmptyCollection_whenValuesIsNull() {
        final Set<String> copy = SetUtils.copyOrEmpty(null);

        assertThat(copy).isEmpty();
    }

    @Test
    void copyOrEmpty_shouldReturnACopy_whenValuesIsGiven() {
        final Set<String> values = new HashSet<>(Set.of("a", "b"));

        final Set<String> copy = SetUtils.copyOrEmpty(values);

        assertThat(copy).isEqualTo(values).isNotSameAs(values);
    }

    @Test
    void orEmpty_shouldReturnTheSameInstance_whenValuesIsGiven() {
        final Set<String> values = new HashSet<>(Set.of("a", "b"));

        final Set<String> result = SetUtils.orEmpty(values);

        assertThat(result).isSameAs(values);
    }

    @Test
    void orEmpty_shouldReturnAnEmptyCollection_whenValuesIsNull() {
        final Set<String> result = SetUtils.orEmpty(null);

        assertThat(result).isEmpty();
    }
}
