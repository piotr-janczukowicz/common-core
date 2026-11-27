package io.github.piotr_janczukowicz.http.client.internal;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireBetween;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.transport.ConcurrencyLimitingHttpTransport;
import io.github.piotr_janczukowicz.http.transport.ConditionalCacheConfiguration;
import io.github.piotr_janczukowicz.http.transport.ConditionalCachingHttpTransport;
import io.github.piotr_janczukowicz.http.transport.HttpTransport;
import io.github.piotr_janczukowicz.http.transport.HttpTransportConfiguration;
import io.github.piotr_janczukowicz.http.transport.MutationPacingHttpTransport;
import io.github.piotr_janczukowicz.http.transport.RateLimitDetector;
import io.github.piotr_janczukowicz.http.transport.RetryConfiguration;
import java.time.Duration;
import java.util.Set;

/**
 * Decorator stack of the vendor clients. Outermost first: the conditional cache, retry with rate-limit waits, pacing of
 * requests that change state, a limit on the requests in flight, and the base transport. Exported only to the modules
 * that build vendor clients; it is not part of the API.
 */
public final class ClientStack {

    private static final int MIN_CONCURRENT_REQUESTS = 1;
    private static final int MAX_CONCURRENT_REQUESTS = 100;

    private ClientStack() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Refuses a transport configuration that a client cannot run with: the limit on requests in flight must be from 1
     * to 100, because a client always keeps that limit.
     *
     * @param transport transport settings to check
     * @throws NullPointerException if {@code transport} is {@code null}
     * @throws IllegalArgumentException if {@code maxConcurrentRequests} is outside 1 to 100
     */
    public static void requireClientConcurrency(final HttpTransportConfiguration transport) {
        requireBetween(
                requireNonNull(transport, "transport").maxConcurrentRequests(),
                MIN_CONCURRENT_REQUESTS,
                MAX_CONCURRENT_REQUESTS,
                "maxConcurrentRequests");
    }

    /**
     * Stacks the decorators over {@code base}. If a decorator refuses an argument, {@code base} is closed and the
     * refusal is rethrown.
     *
     * @param base transport at the bottom of the stack, closed with the stack or on a refusal
     * @param transport limit on requests in flight and the wait for a slot
     * @param retry retry settings
     * @param detector recognizes rate-limited responses
     * @param mutationInterval least time between the starts of two mutations; zero leaves mutations unpaced
     * @param cache limits of the conditional cache
     * @param varyingHeaders request header names whose values key the conditional cache
     * @param retryLayer adds the retry layer over the pacing layer
     * @return the top of the stack, which closes {@code base} when it is closed
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if a decorator refuses its settings
     */
    public static HttpTransport assemble(
            final HttpTransport base,
            final HttpTransportConfiguration transport,
            final RetryConfiguration retry,
            final RateLimitDetector detector,
            final Duration mutationInterval,
            final ConditionalCacheConfiguration cache,
            final Set<String> varyingHeaders,
            final RetryLayer retryLayer) {
        requireNonNull(base, "base");
        try {
            requireNonNull(transport, "transport");
            final HttpTransport retried = requireNonNull(retryLayer, "retryLayer")
                    .apply(
                            MutationPacingHttpTransport.pacedBy(
                                    new ConcurrencyLimitingHttpTransport(
                                            base, transport.maxConcurrentRequests(), transport.maxSlotWait()),
                                    mutationInterval),
                            retry,
                            detector);
            return new ConditionalCachingHttpTransport(retried, cache, varyingHeaders);
        } catch (final RuntimeException rejected) {
            base.close();
            throw rejected;
        }
    }

    /** Wraps a transport in a retry layer. */
    @FunctionalInterface
    public interface RetryLayer {

        /**
         * Wraps {@code delegate} in a retry layer.
         *
         * @param delegate transport to retry
         * @param retry retry settings
         * @param detector recognizes rate-limited responses
         * @return transport that closes {@code delegate} when it is closed
         */
        HttpTransport apply(HttpTransport delegate, RetryConfiguration retry, RateLimitDetector detector);
    }
}
