package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.module.SimpleModule;

class AdapterValidationTest {

    private static final SimpleModule MODULE = new SimpleModule();

    @Test
    void register_shouldThrow_whenArrayOrWrappedArgumentIsNull() {
        final Throwable thrown = catchThrowable(() -> ArrayOrWrappedAdapters.register(
                MODULE, String.class, "items", value -> List.of(), null, List::toString, List::toString));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("wrappedValue");
    }

    @Test
    void register_shouldThrow_whenStringOrObjectArgumentIsNull() {
        final Throwable thrown = catchThrowable(() -> StringOrObjectAdapters.register(
                MODULE, String.class, value -> value, value -> value, null, value -> value, String.class));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("ofScalar");
    }

    @Test
    void register_shouldThrow_whenBranchesAreEmpty() {
        final Throwable thrown = catchThrowable(
                () -> PolymorphicObjectAdapters.register(MODULE, String.class, value -> value, "unknown", List.of()));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("branches");
    }

    @Test
    void branch_shouldThrow_whenWrapIsNull() {
        final Throwable thrown = catchThrowable(() -> PolymorphicObjectAdapters.branch("field", String.class, null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("wrap");
    }

    @Test
    void factories_shouldThrow_whenTextFactoryIsNull() {
        final Throwable thrown = catchThrowable(
                () -> new ScalarUnionReaders.Factories<>(null, value -> "", null, value -> "", false, "message"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
    }
}
