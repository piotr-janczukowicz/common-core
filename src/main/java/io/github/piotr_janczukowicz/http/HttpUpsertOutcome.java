package io.github.piotr_janczukowicz.http;

/** What a create-or-replace call did, as the server reports it by the status of its answer. */
public enum HttpUpsertOutcome {
    /** The server created the resource (HTTP 201). */
    CREATED,
    /** The server replaced an existing resource (any other 2xx, typically HTTP 204). */
    UPDATED;

    /**
     * The outcome of a successful answer.
     *
     * @param statusCode status of the successful response
     * @return {@link #CREATED} for HTTP 201, {@link #UPDATED} for any other status
     */
    public static HttpUpsertOutcome fromStatus(final int statusCode) {
        return HttpStatus.CREATED.hasCode(statusCode) ? CREATED : UPDATED;
    }
}
