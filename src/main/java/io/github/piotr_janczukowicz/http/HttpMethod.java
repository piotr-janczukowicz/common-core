package io.github.piotr_janczukowicz.http;

/** HTTP request method. */
public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS;

    /**
     * Whether requests of this method carry a body, an empty one when the caller supplied none (POST, PUT, PATCH).
     *
     * @return {@code true} if a request of this method is sent with a body
     */
    public boolean expectsBody() {
        return this == POST || this == PUT || this == PATCH;
    }

    /**
     * Whether a request of this method cannot carry a body (GET, HEAD).
     *
     * @return {@code true} if a body is refused
     */
    public boolean isBodyless() {
        return this == GET || this == HEAD;
    }
}
