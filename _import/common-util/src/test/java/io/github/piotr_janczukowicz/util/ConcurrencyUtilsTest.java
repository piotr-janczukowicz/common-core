package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.InterruptedIOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConcurrencyUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(ConcurrencyUtils.class);
    }

    @Test
    void sleepInterruptibly_shouldReturnWithoutSleeping_whenDelayZero() throws InterruptedIOException {
        Thread.currentThread().interrupt();
        try {
            ConcurrencyUtils.sleepInterruptibly(Duration.ZERO, "interrupted");

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void sleepInterruptibly_shouldSleepForDelay_whenDelayPositive() throws InterruptedIOException {
        final long startedNanos = System.nanoTime();

        ConcurrencyUtils.sleepInterruptibly(Duration.ofMillis(50), "interrupted");

        assertThat(Duration.ofNanos(System.nanoTime() - startedNanos)).isGreaterThanOrEqualTo(Duration.ofMillis(20));
    }

    @Test
    void sleepInterruptibly_shouldThrowInterruptedIoWithCause_whenThreadAlreadyInterrupted() {
        Thread.currentThread().interrupt();
        try {
            final Duration delay = Duration.ofSeconds(1);

            final Throwable thrown =
                    catchThrowable(() -> ConcurrencyUtils.sleepInterruptibly(delay, "waiting stopped"));

            final boolean isInterrupted = Thread.interrupted();

            assertSoftly(softly -> {
                softly.assertThat(thrown)
                        .isInstanceOf(InterruptedIOException.class)
                        .hasMessage("waiting stopped")
                        .hasCauseInstanceOf(InterruptedException.class);
                softly.assertThat(isInterrupted).isTrue();
            });
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void sleepInterruptibly_shouldThrowNullPointerException_whenDelayNull() {
        final Duration delay = null;

        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.sleepInterruptibly(delay, "waiting stopped"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("delay must not be null");
    }

    @Test
    void sleepInterruptibly_shouldThrowIllegalArgumentException_whenDelayNegative() {
        final Duration delay = Duration.ofSeconds(-1);

        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.sleepInterruptibly(delay, "waiting stopped"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("delay must not be negative");
    }

    @Test
    void sleepInterruptibly_shouldThrowNullPointerException_whenMessageNull() {
        final String message = null;

        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.sleepInterruptibly(Duration.ZERO, message));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("message must not be null");
    }

    @Test
    void sleepInterruptibly_shouldThrowIllegalArgumentException_whenMessageBlank() {
        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.sleepInterruptibly(Duration.ZERO, " "));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("message must not be blank");
    }

    @Test
    void toInterruptedIo_shouldReturnExceptionWithMessageAndCause() {
        final InterruptedException interrupted = new InterruptedException("stop");

        try {
            final InterruptedIOException failure = ConcurrencyUtils.toInterruptedIo(interrupted, "gave up");

            assertThat(failure).hasMessage("gave up").hasCause(interrupted);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void toInterruptedIo_shouldRestoreInterruptFlag() {
        final InterruptedException interrupted = new InterruptedException("stop");

        try {
            ConcurrencyUtils.toInterruptedIo(interrupted, "gave up");

            assertThat(Thread.interrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void toInterruptedIo_shouldThrowNullPointerException_whenInterruptedNull() {
        final InterruptedException interrupted = null;

        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.toInterruptedIo(interrupted, "gave up"));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("interrupted must not be null");
    }

    @Test
    void toInterruptedIo_shouldThrowIllegalArgumentException_whenMessageBlank() {
        final InterruptedException interrupted = new InterruptedException("stop");

        final Throwable thrown = catchThrowable(() -> ConcurrencyUtils.toInterruptedIo(interrupted, ""));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("message must not be blank");
    }
}
