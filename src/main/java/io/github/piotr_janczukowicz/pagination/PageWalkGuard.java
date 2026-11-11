package io.github.piotr_janczukowicz.pagination;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtLeast;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.uri.HttpUris;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The two rules that keep a walk over server-supplied links finite: a page limit and the refusal to load a page twice.
 * A server that alternates the order of the query parameters, or points a link back at the first request, cannot walk
 * the caller in circles.
 *
 * <p>Call {@link #requireNext(URI, int)} before loading the page a link names, and {@link #markFollowed(URI)} once it
 * loaded, so that a failed load does not count as a visit. The first request counts as visited from the start, so a
 * link that points back at it is caught at once instead of after one extra page. An instance belongs to one walk and is
 * not thread-safe.
 */
public final class PageWalkGuard {

    private final @Nullable Integer maxPages;
    private final Set<String> followed = new HashSet<>();

    /**
     * Guard for one walk.
     *
     * @param maxPages the most pages the walk may read, or {@code null} for no limit
     * @param firstRequest URI of the first request, which counts as visited
     * @throws NullPointerException if {@code firstRequest} is {@code null}
     * @throws IllegalArgumentException if {@code maxPages} is below one
     */
    public PageWalkGuard(final @Nullable Integer maxPages, final URI firstRequest) {
        if (maxPages != null) {
            requireAtLeast(maxPages, 1, "maxPages");
        }
        this.maxPages = maxPages;
        followed.add(visitedKey(requireNonNull(firstRequest, "firstRequest")));
    }

    /**
     * Checks that the page {@code link} names may be loaded.
     *
     * @param link URI of the next page
     * @param pageCount pages read so far
     * @throws PaginationLimitExceededException if {@code pageCount} has reached {@code maxPages}; checked first, so a
     *     walk never silently stops early
     * @throws PaginationLoopException if {@code link} names a page that was already followed; the message names the
     *     link without its query
     */
    public void requireNext(final URI link, final int pageCount) {
        requireNonNull(link, "link");
        if (maxPages != null && pageCount >= maxPages) {
            throw new PaginationLimitExceededException(maxPages, pageCount);
        }
        if (followed.contains(visitedKey(link))) {
            throw new PaginationLoopException(
                    "Link rel=\"next\" repeated after " + pageCount + " page(s): " + HttpUris.describeSafe(link));
        }
    }

    /**
     * Records that the page {@code link} names was loaded.
     *
     * @param link URI of the page that was loaded
     */
    public void markFollowed(final URI link) {
        followed.add(visitedKey(requireNonNull(link, "link")));
    }

    // The same page with its query parameters in another order is the same page.
    private static String visitedKey(final URI uri) {
        final String rawQuery = uri.getRawQuery();
        final StringBuilder key = new StringBuilder(String.valueOf(uri.getScheme()))
                .append("://")
                .append(uri.getRawAuthority())
                .append(uri.getRawPath());
        if (rawQuery != null) {
            final String[] parameters = rawQuery.split("&");
            Arrays.sort(parameters);
            key.append('?').append(String.join("&", parameters));
        }
        return key.toString();
    }
}
