package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

class PaginationConfigurationTest {

    @Test
    void defaults_shouldUseExpectedValues_whenCalled() {
        final PaginationConfiguration config = PaginationConfiguration.defaults();

        assertSoftly(softly -> {
            softly.assertThat(config.maxPages()).isNull();
            softly.assertThat(config.perPage()).isEqualTo(100);
            softly.assertThat(config.perPage()).isEqualTo(100);
        });
    }

    @Test
    void build_shouldOverrideAllFields_whenBuilderUsed() {
        final PaginationConfiguration config =
                PaginationConfiguration.builder().maxPages(5).perPage(50).build();

        assertSoftly(softly -> {
            softly.assertThat(config.maxPages()).isEqualTo(5);
            softly.assertThat(config.perPage()).isEqualTo(50);
        });
    }

    @Test
    void unlimitedPages_shouldClearCap_whenCalledAfterMaxPages() {
        final PaginationConfiguration config =
                PaginationConfiguration.builder().maxPages(3).unlimitedPages().build();

        assertThat(config.maxPages()).isNull();
    }

    @Test
    void constructor_shouldTreatMaxPagesAsUnlimited_whenMaxPagesIsNull() {
        final PaginationConfiguration config = new PaginationConfiguration(null, 30);

        assertThat(config.maxPages()).isNull();
    }

    @Test
    void constructor_shouldThrow_whenMaxPagesBelowMinimum() {
        final Throwable thrown = catchThrowable(() -> new PaginationConfiguration(0, 30));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxPages");
    }

    @Test
    void constructor_shouldThrow_whenPerPageBelowMinimum() {
        final Throwable thrown = catchThrowable(() -> new PaginationConfiguration(10, 0));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("perPage");
    }

    @Test
    void constructor_shouldThrow_whenPerPageAboveMaximum() {
        final Throwable thrown = catchThrowable(() -> new PaginationConfiguration(10, 101));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("perPage");
    }
}
