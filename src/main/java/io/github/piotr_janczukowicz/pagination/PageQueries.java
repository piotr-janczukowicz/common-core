package io.github.piotr_janczukowicz.pagination;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireAtMost;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.uri.HttpUris;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Builds the query of a page request from a base query and the settings of a walk. */
public final class PageQueries {

    private PageQueries() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Picks the page size to send: the explicit one, or the configured one, never above {@code maxPerPage}.
     *
     * @param explicitPerPage the size the request names, or {@code null} if it names none
     * @param configuration default page size
     * @param maxPerPage the largest size the endpoint accepts
     * @return the size to send
     * @throws NullPointerException if {@code configuration} is {@code null}
     * @throws IllegalArgumentException if the explicit size is above {@code maxPerPage}; the message is {@code perPage
     *     must be at most <maxPerPage>}
     */
    public static int boundPerPage(
            @Nullable final Integer explicitPerPage,
            final PaginationConfiguration configuration,
            final int maxPerPage) {
        requireNonNull(configuration, "configuration");

        if (explicitPerPage == null) {
            return Math.min(configuration.perPage(), maxPerPage);
        }
        return requireAtMost(explicitPerPage, maxPerPage, "perPage");
    }

    /**
     * Copies {@code baseQuery} and sets each of {@code parameters} as a single-valued entry, replacing an entry of the
     * same name. The order of {@code baseQuery} is kept, then new names follow in the order of {@code parameters}.
     *
     * @param baseQuery endpoint-specific filters
     * @param parameters parameters to set, name to value
     * @return new unmodifiable query map
     * @throws NullPointerException if an argument is {@code null}
     */
    public static Map<String, List<String>> withParameters(
            final Map<String, List<String>> baseQuery, final Map<String, String> parameters) {
        requireNonNull(parameters, "parameters");

        final Map<String, List<String>> query = HttpUris.copyQueryValues(baseQuery);
        parameters.forEach((name, value) -> query.put(name, List.of(value)));
        return Collections.unmodifiableMap(query);
    }
}
