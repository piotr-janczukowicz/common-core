package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationUtilsCollectionTest {

    @Test
    void requireSizeAtMost_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeAtMost(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a", "b", "c");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at most 2 elements");
    }

    @Test
    void requireSizeAtMost_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeAtMost(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtMost_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeAtMost(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMost_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2, "c", 3);

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMost(values, 2, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must have at most 2 entries");
    }

    @Test
    void requireSizeAtMost_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeAtMost(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtLeast_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeAtLeast(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at least 2 elements");
    }

    @Test
    void requireSizeAtLeast_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeAtLeast(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtLeast_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeAtLeast(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeast_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeast(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at least 2 entries");
    }

    @Test
    void requireSizeAtLeast_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeAtLeast(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeBetween_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeBetween(values, 2, 3, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetween_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeBetween(values, 2, 3, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 2 and 3 elements");
    }

    @Test
    void requireSizeBetween_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeBetween(values, 2, 3, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeBetween_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeBetween(values, 2, 3, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetween_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeBetween(values, 2, 3, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 2 and 3 entries");
    }

    @Test
    void requireSizeBetween_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeBetween(values, 2, 3, "items");

        assertThat(result).isNull();
    }

    @Test
    void rejectEmpty_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a");

        final List<String> result = ValidationUtils.rejectEmpty(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void rejectEmpty_shouldThrow_whenInvalid() {
        final List<String> values = List.of();

        final Throwable thrown = catchThrowable(() -> ValidationUtils.rejectEmpty(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not be empty");
    }

    @Test
    void rejectEmpty_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.rejectEmpty(values, "items");

        assertThat(result).isNull();
    }

    @Test
    void rejectEmpty_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Map<String, Integer> result = ValidationUtils.rejectEmpty(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void rejectEmpty_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of();

        final Throwable thrown = catchThrowable(() -> ValidationUtils.rejectEmpty(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not be empty");
    }

    @Test
    void rejectEmpty_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.rejectEmpty(values, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireUniqueItems_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireUniqueItems(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireUniqueItems_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a", "a");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireUniqueItems(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not contain duplicates");
    }

    @Test
    void requireUniqueItems_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireUniqueItems(values, "items");

        assertThat(result).isNull();
    }
}
