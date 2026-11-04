package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNegative;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.io.InterruptedIOException;
import java.time.Duration;

/**
 * Thread interruption helpers for blocking code that reports an interrupt as an I/O failure.
 *
 * <p>Both methods restore the interrupt flag, so the interrupt stays visible to code above the caller even after the
 * exception has been handled.
 */
@SuppressWarnings("PMD.DoNotUseThreads") // Handling interrupts of blocking calls is this class's purpose.
public final class ConcurrencyUtils {

    private ConcurrencyUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sleeps for {@code delay}. An interrupt is reported as an {@link InterruptedIOException}.
     *
     * <p>A zero delay returns at once without sleeping, so an interrupt flag already set stays set. Otherwise, the
     * thread sleeps; if it is interrupted, or was interrupted before the call, the interrupt flag is restored and an
     * {@link InterruptedIOException} with {@code message} is thrown, caused by the {@link InterruptedException}.
     *
     * @param delay how long to sleep, zero or positive
     * @param message exception text used if the sleep is interrupted
     * @throws NullPointerException if {@code delay} or {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code delay} is negative or {@code message} is blank
     * @throws InterruptedIOException if the thread is interrupted
     */
    public static void sleepInterruptibly(final Duration delay, final String message) throws InterruptedIOException {
        requireNonNull(delay, "delay");
        requireNonNegative(delay, "delay");
        requireNotBlank(message, "message");

        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException interrupted) {
            throw toInterruptedIo(interrupted, message);
        }
    }

    /**
     * Restores the interrupt flag of the current thread and creates the matching I/O exception.
     *
     * <p>The exception is returned, not thrown, so the caller can add suppressed exceptions or wrap it first:
     * {@code throw toInterruptedIo(e, "...")}.
     *
     * @param interrupted the interrupt that was caught; becomes the cause of the result
     * @param message exception text of the result
     * @return an {@link InterruptedIOException} with {@code message} and {@code interrupted} as its cause
     * @throws NullPointerException if {@code interrupted} or {@code message} is {@code null}
     * @throws IllegalArgumentException if {@code message} is blank
     */
    public static InterruptedIOException toInterruptedIo(final InterruptedException interrupted, final String message) {
        requireNonNull(interrupted, "interrupted");
        requireNotBlank(message, "message");

        Thread.currentThread().interrupt();
        final InterruptedIOException failure = new InterruptedIOException(message);
        failure.initCause(interrupted);
        return failure;
    }
}
