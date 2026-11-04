package io.github.piotr_janczukowicz.util;

import java.io.IOException;
import java.io.Serial;

/**
 * A stream held more bytes than the limit passed to {@link IoUtils#readAllBounded}. Not final, so callers can subclass
 * it.
 */
public class BoundedReadLimitException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long limit;

    /**
     * Failure for a stream longer than {@code limit}.
     *
     * @param limit the byte limit that was exceeded
     */
    public BoundedReadLimitException(final long limit) {
        super("Stream exceeds the limit of %d bytes".formatted(limit));
        this.limit = limit;
    }

    /**
     * Byte limit that was exceeded.
     *
     * @return limit in bytes
     */
    public long limit() {
        return limit;
    }
}
