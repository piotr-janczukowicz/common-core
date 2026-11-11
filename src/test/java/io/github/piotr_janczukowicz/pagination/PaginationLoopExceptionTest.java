package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PaginationLoopExceptionTest {

    @Test
    void constructor_shouldKeepTheMessage_whenCreated() {
        final PaginationLoopException thrown = new PaginationLoopException("repeated after 3 page(s)");

        assertThat(thrown).isInstanceOf(PaginationException.class).hasMessage("repeated after 3 page(s)");
    }

    @Test
    void paginationException_shouldKeepTheCause_whenCreatedWithOne() {
        final IllegalStateException cause = new IllegalStateException("boom");

        final PaginationException thrown = new PaginationException("outside the base", cause);

        assertThat(thrown).hasMessage("outside the base").hasCause(cause);
    }
}
