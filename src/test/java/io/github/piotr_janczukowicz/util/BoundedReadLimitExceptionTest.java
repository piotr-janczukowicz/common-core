package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BoundedReadLimitExceptionTest {

    @Test
    void limit_shouldReturnLimitPassedToConstructor() {
        final BoundedReadLimitException exception = new BoundedReadLimitException(4096);

        final long limit = exception.limit();

        assertThat(limit).isEqualTo(4096);
    }

    @Test
    void getMessage_shouldNameLimit() {
        final BoundedReadLimitException exception = new BoundedReadLimitException(4096);

        assertThat(exception).hasMessage("Stream exceeds the limit of 4096 bytes");
    }
}
