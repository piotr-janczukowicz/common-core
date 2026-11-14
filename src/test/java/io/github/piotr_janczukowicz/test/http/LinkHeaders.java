package io.github.piotr_janczukowicz.test.http;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.net.URI;

/** Builds {@code Link} header values for tests of paged APIs. */
public final class LinkHeaders {

    private LinkHeaders() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * A {@code Link} header value that names {@code uri} as the next page.
     *
     * @param uri target of the link
     * @return {@code <uri>; rel="next"}
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static String next(final URI uri) {
        return next(requireNonNull(uri, "uri").toString());
    }

    /**
     * A {@code Link} header value that names {@code uri} as the next page.
     *
     * @param uri target of the link, as text
     * @return {@code <uri>; rel="next"}
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static String next(final String uri) {
        return "<" + requireNonNull(uri, "uri") + ">; rel=\"next\"";
    }
}
