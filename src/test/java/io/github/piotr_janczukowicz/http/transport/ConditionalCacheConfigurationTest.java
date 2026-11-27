package io.github.piotr_janczukowicz.http.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ConditionalCacheConfigurationTest {

    @Test
    void defaults_shouldUseExpectedLimits_whenCalled() {
        final ConditionalCacheConfiguration cache = ConditionalCacheConfiguration.defaults();

        assertSoftly(softly -> {
            softly.assertThat(cache.maxEntries()).isEqualTo(64);
            softly.assertThat(cache.maxBodyBytes()).isEqualTo(1024 * 1024);
            softly.assertThat(cache.maxBytes()).isEqualTo(4L * 1024 * 1024);
        });
    }

    @Test
    void build_shouldOverrideAllLimits_whenBuilderUsed() {
        final ConditionalCacheConfiguration cache = ConditionalCacheConfiguration.builder()
                .maxEntries(5)
                .maxBodyBytes(6)
                .maxBytes(7)
                .build();

        assertThat(cache).isEqualTo(new ConditionalCacheConfiguration(5, 6, 7L));
    }

    @Test
    void constructor_shouldAcceptOne_whenEveryLimitIsAtItsMinimum() {
        final ConditionalCacheConfiguration cache = new ConditionalCacheConfiguration(1, 1, 1L);

        assertThat(cache.maxEntries()).isEqualTo(1);
    }

    @Test
    void constructor_shouldThrow_whenMaxEntriesBelowOne() {
        final Throwable thrown = catchThrowable(() -> new ConditionalCacheConfiguration(0, 1, 1L));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxEntries");
    }

    @Test
    void constructor_shouldThrow_whenMaxBodyBytesBelowOne() {
        final Throwable thrown = catchThrowable(() -> new ConditionalCacheConfiguration(1, 0, 1L));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxBodyBytes");
    }

    @Test
    void constructor_shouldThrow_whenMaxBytesBelowOne() {
        final Throwable thrown = catchThrowable(() -> new ConditionalCacheConfiguration(1, 1, 0L));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxBytes");
    }

    @Test
    void build_shouldThrow_whenALimitIsBelowOne() {
        final Throwable thrown = catchThrowable(
                () -> ConditionalCacheConfiguration.builder().maxBytes(-1).build());

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxBytes");
    }
}
