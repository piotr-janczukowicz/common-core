package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImmutableCopiesTest {

    @Test
    void list_shouldReturnEmpty_whenSourceIsEmpty() {
        final List<String> result = ImmutableCopies.list(new ArrayList<>());

        assertThat(result).isEmpty();
    }

    @Test
    void set_shouldReturnEmpty_whenSourceIsEmpty() {
        final Set<String> result = ImmutableCopies.set(new ArrayList<>());

        assertThat(result).isEmpty();
    }

    @Test
    void map_shouldReturnEmpty_whenSourceIsEmpty() {
        final Map<String, Integer> result = ImmutableCopies.map(new HashMap<>());

        assertThat(result).isEmpty();
    }

    @Test
    void list_shouldKeepOrder_whenSourceHasNoNulls() {
        final List<String> result = ImmutableCopies.list(new ArrayList<>(List.of("a", "b")));

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void set_shouldDeduplicate_whenSourceHasNoNulls() {
        final Set<String> result = ImmutableCopies.set(new ArrayList<>(List.of("a", "a")));

        assertThat(result).containsExactly("a");
    }

    @Test
    void list_shouldCopyElements_whenSourceIsNotAList() {
        final List<String> result = ImmutableCopies.list(new LinkedHashSet<>(List.of("a", "b")));

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    void set_shouldWrapSource_whenSourceIsASet() {
        final Set<String> result = ImmutableCopies.set(new HashSet<>(List.of("a", "b")));

        assertThat(result).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void map_shouldCopyEntries_whenSourceHasNoNulls() {
        final Map<String, Integer> source = new HashMap<>(Map.of("a", 1));

        final Map<String, Integer> result = ImmutableCopies.map(source);

        assertThat(result).containsExactly(Map.entry("a", 1));
    }

    @Test
    void list_shouldKeepNulls_whenSourceHasNulls() {
        final List<String> result = ImmutableCopies.list(Arrays.asList("a", null, "b"));

        assertThat(result).containsExactly("a", null, "b");
    }

    @Test
    void set_shouldKeepNull_whenSourceHasNulls() {
        final Set<String> result = ImmutableCopies.set(Arrays.asList("a", null, "a"));

        assertThat(result).containsExactly("a", null);
    }

    @Test
    void map_shouldKeepNullValues_whenSourceHasNullValues() {
        final Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", null);

        final Map<String, Integer> result = ImmutableCopies.map(source);

        assertThat(result).hasSize(2).containsEntry("a", 1).containsEntry("b", null);
    }

    @Test
    void list_shouldBeUnmodifiable_whenSourceHasNulls() {
        final List<String> copy = ImmutableCopies.list(Arrays.asList("a", null));

        final Throwable thrown = catchThrowable(() -> copy.add("c"));

        assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_shouldRejectInstantiation() throws Exception {
        final Constructor<ImmutableCopies> constructor = ImmutableCopies.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
