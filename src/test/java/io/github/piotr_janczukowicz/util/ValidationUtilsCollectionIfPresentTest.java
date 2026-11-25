package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationUtilsCollectionIfPresentTest {

    @Test
    void requireSizeAtMostIfPresent_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMostIfPresent_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a", "b", "c");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at most 2 elements");
    }

    @Test
    void requireSizeAtMostIfPresent_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtMostIfPresent_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtMostIfPresent_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2, "c", 3);

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must have at most 2 entries");
    }

    @Test
    void requireSizeAtMostIfPresent_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeAtMostIfPresent(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at least 2 elements");
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have at least 2 entries");
    }

    @Test
    void requireSizeAtLeastIfPresent_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeAtLeastIfPresent(values, 2, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeBetweenIfPresent_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetweenIfPresent_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a");

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 2 and 3 elements");
    }

    @Test
    void requireSizeBetweenIfPresent_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireSizeBetweenIfPresent_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1, "b", 2);

        final Map<String, Integer> result = ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireSizeBetweenIfPresent_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Throwable thrown =
                catchThrowable(() -> ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items"));

        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("items must have between 2 and 3 entries");
    }

    @Test
    void requireSizeBetweenIfPresent_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireSizeBetweenIfPresent(values, 2, 3, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireNonEmptyIfPresent_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a");

        final List<String> result = ValidationUtils.requireNonEmptyIfPresent(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireNonEmptyIfPresent_shouldThrow_whenInvalid() {
        final List<String> values = List.of();

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmptyIfPresent(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not be empty");
    }

    @Test
    void requireNonEmptyIfPresent_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireNonEmptyIfPresent(values, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireNonEmptyIfPresent_shouldReturnMap_whenMapValid() {
        final Map<String, Integer> values = Map.of("a", 1);

        final Map<String, Integer> result = ValidationUtils.requireNonEmptyIfPresent(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireNonEmptyIfPresent_shouldThrow_whenMapInvalid() {
        final Map<String, Integer> values = Map.of();

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireNonEmptyIfPresent(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not be empty");
    }

    @Test
    void requireNonEmptyIfPresent_shouldReturnNull_whenMapNull() {
        final Map<String, Integer> values = null;

        final Map<String, Integer> result = ValidationUtils.requireNonEmptyIfPresent(values, "items");

        assertThat(result).isNull();
    }

    @Test
    void requireUniqueItemsIfPresent_shouldReturnValue_whenWithinBounds() {
        final List<String> values = List.of("a", "b");

        final List<String> result = ValidationUtils.requireUniqueItemsIfPresent(values, "items");

        assertThat(result).isSameAs(values);
    }

    @Test
    void requireUniqueItemsIfPresent_shouldThrow_whenInvalid() {
        final List<String> values = List.of("a", "a");

        final Throwable thrown = catchThrowable(() -> ValidationUtils.requireUniqueItemsIfPresent(values, "items"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("items must not contain duplicates");
    }

    @Test
    void requireUniqueItemsIfPresent_shouldReturnNull_whenNull() {
        final List<String> values = null;

        final List<String> result = ValidationUtils.requireUniqueItemsIfPresent(values, "items");

        assertThat(result).isNull();
    }
}
