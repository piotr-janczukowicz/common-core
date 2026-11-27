package io.github.piotr_janczukowicz.http.client.internal;

import io.github.piotr_janczukowicz.http.internal.RetryingTransportAccess;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import io.github.piotr_janczukowicz.http.transport.RetryingHttpTransport;
import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Retrying transports that take their pause, clock and random source from the caller. Exported only to the modules that
 * build vendor clients; it is not part of the API.
 */
public final class RetryingTransports {

    private RetryingTransports() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds a retrying transport that pauses with {@code sleeper} and reads time from {@code clock}, which serves both
     * the retry policy and the rate-limit gate. The jitter is random.
     *
     * @param delegate transport that performs each attempt
     * @param configuration retry policy data
     * @param detector recognizes rate-limited responses
     * @param sleeper pause between attempts
     * @param clock time source for reset times and gate blocks
     * @return the transport
     * @throws NullPointerException if an argument is {@code null}
     */
    public static RetryingHttpTransport create(
            final HttpTransport delegate,
            final RetryConfiguration configuration,
            final RateLimitDetector detector,
            final RetryingHttpTransport.Sleeper sleeper,
            final Clock clock) {
        return create(
                delegate,
                configuration,
                detector,
                sleeper,
                clock,
                () -> ThreadLocalRandom.current().nextLong());
    }

    /**
     * Builds a retrying transport that pauses with {@code sleeper}, reads time from {@code clock} and draws its jitter
     * from {@code random}.
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
        return RetryingTransportAccess.create(delegate, configuration, detector, sleeper, clock, random);
    }
}
