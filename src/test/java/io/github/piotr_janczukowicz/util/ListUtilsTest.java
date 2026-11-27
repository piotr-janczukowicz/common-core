package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(ListUtils.class);
    }

    @Test
    void copyOf_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final List<String> values = new ArrayList<>(List.of("a", "b"));

        final List<String> copy = ListUtils.copyOf(values, "values");
        values.add("c");

        assertSoftly(softly -> {
            softly.assertThat(copy).containsExactly("a", "b");
            softly.assertThat(catchThrowable(() -> copy.add("d"))).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copyOf_shouldThrowWithTheName_whenValuesIsNull() {
        final Throwable thrown = catchThrowable(() -> ListUtils.copyOf(null, "labels"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("labels");
    }

    @Test
    void copyOf_shouldThrow_whenValuesHasANullElement() {
        final List<String> values = Arrays.asList("a", null);

        final Throwable thrown = catchThrowable(() -> ListUtils.copyOf(values, "values"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copy_shouldReturnNull_whenValuesIsNull() {
        final List<String> copy = ListUtils.copy(null);

        assertThat(copy).isNull();
    }

    @Test
    void copy_shouldReturnAnEmptyList_whenValuesIsEmpty() {
        final List<String> copy = ListUtils.copy(List.of());

        assertThat(copy).isNotNull().isEmpty();
    }

    @Test
    void copy_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final List<String> values = new ArrayList<>(List.of("a", "b"));

        final List<String> copy = ListUtils.copy(values);
        values.add("c");

        assertSoftly(softly -> {
            softly.assertThat(copy).containsExactly("a", "b");
            softly.assertThat(catchThrowable(() -> copy.add("d"))).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copy_shouldThrow_whenValuesHasANullElement() {
        final List<String> values = Arrays.asList("a", null);

        final Throwable thrown = catchThrowable(() -> ListUtils.copy(values));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void copyNonNull_shouldReturnAnEmptyList_whenValuesIsNull() {
        final List<String> copy = ListUtils.copyNonNull(null);

        assertThat(copy).isEmpty();
    }

    @Test
    void copyNonNull_shouldReturnAnUnmodifiableCopy_whenValuesIsGiven() {
        final List<String> values = new ArrayList<>(List.of("a", "b"));

        final List<String> copy = ListUtils.copyNonNull(values);
        values.add("c");

        assertSoftly(softly -> {
            softly.assertThat(copy).containsExactly("a", "b");
            softly.assertThat(catchThrowable(() -> copy.add("d"))).isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void copyNonNull_shouldThrow_whenValuesHasANullElement() {
        final List<String> values = Arrays.asList("a", null);

        final Throwable thrown = catchThrowable(() -> ListUtils.copyNonNull(values));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void orEmpty_shouldReturnTheSameList_whenValuesIsGiven() {
        final List<String> values = new ArrayList<>(List.of("a"));

        final List<String> result = ListUtils.orEmpty(values);

        assertThat(result).isSameAs(values);
    }

    @Test
    void orEmpty_shouldReturnAnEmptyList_whenValuesIsNull() {
        final List<String> result = ListUtils.orEmpty(null);

        assertThat(result).isEmpty();
    }
}
