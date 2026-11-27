package io.github.piotr_janczukowicz.http.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.http.transport.RetryingHttpTransport;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;
import java.util.random.RandomGenerator;

/**
 * Builds a {@link RetryingHttpTransport} with a given pause, clock and random source without making those constructors
 * part of the public API.
 *
 * <p>This package is not exported from the module.
 */
public final class RetryingTransportAccess {

    private static final AtomicReference<Factory> FACTORY = new AtomicReference<>();

    private RetryingTransportAccess() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers the factory; called once by {@link RetryingHttpTransport}.
     *
     * @param factory builds the transport
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public static void register(final Factory factory) {
        FACTORY.set(requireNonNull(factory, "factory"));
    }

    /**
     * Builds a retrying transport with the given pause, clock and random source.
     *
     * @param delegate transport that performs each attempt
     * @param configuration retry policy data
     * @param detector recognizes rate-limited responses
     * @param sleeper pause between attempts
     * @param clock time source for reset times and gate blocks
     * @param random jitter source
     * @return the transport
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RetryingHttpTransport create(
            final HttpTransport delegate,
            final RetryConfiguration configuration,
            final RateLimitDetector detector,
            final RetryingHttpTransport.Sleeper sleeper,
            final Clock clock,
            final RandomGenerator random) {
        if (FACTORY.get() == null) {
            load(RetryingHttpTransport.class.getName());
        }
        return FACTORY.get().create(delegate, configuration, detector, sleeper, clock, random);
    }

    static void load(final String className) {
        try {
            Class.forName(className);
        } catch (final ClassNotFoundException missing) {
            throw new IllegalStateException("Cannot load " + className, missing);
        }
    }

    /** Builds a retrying transport that takes its pause, clock and randomness from the caller. */
    @FunctionalInterface
    public interface Factory {

        /**
         * Builds the transport.
         *
         * @param delegate transport that performs each attempt
         * @param configuration retry policy data
         * @param detector recognizes rate-limited responses
         * @param sleeper pause between attempts
         * @param clock time source for reset times and gate blocks
         * @param random jitter source
         * @return the transport
         */
        RetryingHttpTransport create(
                HttpTransport delegate,
                RetryConfiguration configuration,
                RateLimitDetector detector,
                RetryingHttpTransport.Sleeper sleeper,
                Clock clock,
                RandomGenerator random);
    }
}
