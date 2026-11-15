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
     * @throws PaginationException if an absolute {@code link} is not an {@code http} or {@code https} URL with a host,
     *     or a relative one cannot be resolved against {@code currentRequest}; the message names the link without its
     *     query
     */
    public static URI absolute(final URI currentRequest, final URI link) {
        requireNonNull(currentRequest, "currentRequest");
        requireNonNull(link, "link");
        if (link.isAbsolute()) {
            if (!HttpUris.isHttpUrl(link)) {
                throw new PaginationException(
                        "Link rel=\"next\" is not an http(s) URL: " + HttpUris.describeSafe(link));
            }
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
     * Requires {@code link} to be on the origin of {@code base}, so that a walk never sends its credentials to the host
     * that a server names in {@code Link rel="next"}. {@link #absolute(URI, URI)} does not check this.
     *
     * @param base base URI of the API
     * @param link absolute target of {@code Link rel="next"}
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code base} has no scheme or no host
     * @throws PaginationException if {@code link} has no scheme or no host, or another origin than {@code base}; the
     *     message names both origins without query or user info
     */
    public static void requireSameOrigin(final URI base, final URI link) {
        requireNonNull(base, "base");
        requireNonNull(link, "link");
        final String baseOrigin = HttpUris.originOf(base);
        if (!HttpUris.hasOrigin(link) || !baseOrigin.equals(HttpUris.originOf(link))) {
            throw new PaginationException(
                    "Link rel=\"next\" leaves the origin " + baseOrigin + ": " + HttpUris.describeSafe(link));
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
        final String baseOrigin = HttpUris.originOf(base);
        if (!HttpUris.hasOrigin(link) || baseOrigin.equals(HttpUris.originOf(link))) {
            return false;
        }
        final String basePath = withTrailingSlash(base.normalize().getPath());
        return withTrailingSlash(link.normalize().getPath()).startsWith(basePath);
    }

    private static String withTrailingSlash(final String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
