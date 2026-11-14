package io.github.piotr_janczukowicz.pagination;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.http.uri.HttpUris;
import java.net.URI;

/** Reads the {@code Link rel="next"} targets of a paged API: resolving a relative one, and spotting a foreign one. */
public final class PageLinks {

    private PageLinks() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * The absolute form of {@code link}: {@code link} itself when it is absolute, otherwise {@code link} resolved
     * against the URI of the request that returned it.
     *
     * @param currentRequest URI of the request whose response carried the link
     * @param link target of {@code Link rel="next"}
     * @return an absolute URI
     * @throws NullPointerException if an argument is {@code null}
     * @throws PaginationException if a relative {@code link} cannot be resolved against {@code currentRequest}; the
     *     message names the link without its query
     */
    public static URI absolute(final URI currentRequest, final URI link) {
        requireNonNull(currentRequest, "currentRequest");
        requireNonNull(link, "link");
        if (link.isAbsolute()) {
            return link;
        }
        try {
            return HttpUris.resolveReference(currentRequest, link.toString());
        } catch (final IllegalArgumentException ex) {
            throw new PaginationException(
                    "Link rel=\"next\" cannot be resolved against the request URI: " + HttpUris.describeSafe(link), ex);
        }
    }

    /**
     * Whether {@code link} names the API under another origin than {@code base} does, such as a server that links to
     * its own internal host name. The path of {@code link} must start with the path of {@code base}: a link that leaves
     * the base path as well is not the same API under another name. A walk uses this to add a hint about the configured
     * host name to the message that refuses the link.
     *
     * @param base base URI of the API
     * @param link absolute target of {@code Link rel="next"}
     * @return {@code true} if {@code link} has a scheme and a host, its origin differs from that of {@code base}, and
     *     its path is under the path of {@code base}; {@code false} otherwise, also when {@code link} has no host
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code base} has no scheme or no host
     */
    public static boolean differsOnlyByOrigin(final URI base, final URI link) {
        requireNonNull(base, "base");
        requireNonNull(link, "link");
        return HttpUris.hasOrigin(link)
                && link.getPath().startsWith(base.getPath())
                && !HttpUris.originOf(base).equals(HttpUris.originOf(link));
    }
}
