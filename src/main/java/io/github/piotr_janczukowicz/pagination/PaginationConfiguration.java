package io.github.piotr_janczukowicz.pagination;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtLeast;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtMost;

import org.jspecify.annotations.Nullable;

/**
 * Page-follow cap and default {@code per_page} for list pagination.
 *
 * <p>{@code maxPages} is {@code null} for an unlimited walk, or at least 1 when set. {@code perPage} is between 1 and
 * {@link #MAX_PER_PAGE}. {@code perPage} is the default sent when a list request omits an explicit size; it does not
 * cap an explicit {@code per_page} (the endpoint maximum does).
 *
 * <p><strong>No page cap by default.</strong> {@link #defaults()} leaves {@code maxPages} unset, so a walk that follows
 * every page continues until the server stops sending a next link. That is one request per page against any rate limit,
 * and collecting every item buffers all of them. For endpoints that can return very large lists configure a
 * {@code maxPages}; the walk then fails rather than truncating silently.
 *
 * @param maxPages inclusive maximum HTTP pages per walk, or {@code null} for no cap
 * @param perPage default {@code per_page} when a request does not set one
 */
public record PaginationConfiguration(@Nullable Integer maxPages, int perPage) {

    /** Inclusive maximum {@code per_page} a list endpoint accepts. */
    public static final int MAX_PER_PAGE = 100;

    public PaginationConfiguration {
        if (maxPages != null) {
            requireAtLeast(maxPages, 1, "maxPages");
        }
        requireAtLeast(perPage, 1, "perPage");
        requireAtMost(perPage, MAX_PER_PAGE, "perPage");
    }

    /**
     * Configuration with no page-follow cap and a default {@code per_page} of 100.
     *
     * @return default configuration
     */
    public static PaginationConfiguration defaults() {
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

    /** Builds a {@link PaginationConfiguration}. Unset fields keep the {@link #defaults()} values. */
    public static final class Builder {
        private @Nullable Integer maxPages;
        private int perPage = 100;

        private Builder() {}

        /**
         * Sets the inclusive maximum HTTP pages per walk.
         *
         * @param maxPages maximum pages to follow
         * @return this builder
         */
        public Builder maxPages(final int maxPages) {
            this.maxPages = maxPages;
            return this;
        }

        /**
         * Clears the page-follow cap so every next link is followed.
         *
         * @return this builder
         */
        @SuppressWarnings("PMD.NullAssignment")
        public Builder unlimitedPages() {
            this.maxPages = null;
            return this;
        }

        /**
         * Sets the default {@code per_page} when a request does not set one.
         *
         * @param perPage default {@code per_page} query value
         * @return this builder
         */
        public Builder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return configuration from this builder
         * @throws IllegalArgumentException if {@code maxPages} is below 1 or {@code perPage} is outside
         *     1..{@link PaginationConfiguration#MAX_PER_PAGE}
         */
        public PaginationConfiguration build() {
            return new PaginationConfiguration(maxPages, perPage);
        }
    }
}
