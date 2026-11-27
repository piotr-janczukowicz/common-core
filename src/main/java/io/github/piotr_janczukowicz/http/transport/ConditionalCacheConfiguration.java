package io.github.piotr_janczukowicz.http.transport;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtLeast;

/**
 * Limits of the response cache that {@link ConditionalCachingHttpTransport} keeps. Every limit is at least one, so the
 * cache cannot be switched off with a limit; the constructor throws {@link IllegalArgumentException} for a limit below
 * one.
 *
 * @param maxEntries most responses kept; the least recently used one is dropped beyond it
 * @param maxBodyBytes largest body kept for one response; a larger response is passed on but not kept
 * @param maxBytes most bytes of bodies kept in all; the least recently used entries are dropped until the bodies kept
 *     fit
 */
public record ConditionalCacheConfiguration(int maxEntries, int maxBodyBytes, long maxBytes) {

    private static final int DEFAULT_MAX_ENTRIES = 64;
    private static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;
    private static final long DEFAULT_MAX_BYTES = 4L * 1024 * 1024;

    public ConditionalCacheConfiguration {
        requireAtLeast(maxEntries, 1, "maxEntries");
        requireAtLeast(maxBodyBytes, 1, "maxBodyBytes");
        requireAtLeast(maxBytes, 1L, "maxBytes");
    }

    /**
     * Limits of 64 responses, 1 MiB per body and 4 MiB of bodies in all.
     *
     * @return default limits
     */
    public static ConditionalCacheConfiguration defaults() {
        return builder().build();
    }

    /**
     * Builder initialized to {@link #defaults()}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builds a {@link ConditionalCacheConfiguration}. Unset limits keep the {@link #defaults()} values. */
    public static final class Builder {
        private int maxEntries = DEFAULT_MAX_ENTRIES;
        private int maxBodyBytes = DEFAULT_MAX_BODY_BYTES;
        private long maxBytes = DEFAULT_MAX_BYTES;

        private Builder() {}

        /**
         * Sets the most responses kept. Defaults to 64.
         *
         * @param maxEntries limit of at least 1
         * @return this builder
         */
        public Builder maxEntries(final int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        /**
         * Sets the largest body kept for one response. Defaults to 1 MiB.
         *
         * @param maxBodyBytes limit of at least 1
         * @return this builder
         */
        public Builder maxBodyBytes(final int maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
            return this;
        }

        /**
         * Sets the most bytes of bodies kept in all. Defaults to 4 MiB.
         *
         * @param maxBytes limit of at least 1
         * @return this builder
         */
        public Builder maxBytes(final long maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }

        /**
         * Builds the limits.
         *
         * @return limits from this builder
         * @throws IllegalArgumentException if a limit is below one
         */
        public ConditionalCacheConfiguration build() {
            return new ConditionalCacheConfiguration(maxEntries, maxBodyBytes, maxBytes);
        }
    }
}
