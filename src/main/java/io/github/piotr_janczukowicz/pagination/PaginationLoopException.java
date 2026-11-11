package io.github.piotr_janczukowicz.pagination;

import java.io.Serial;

/** A page walk would never end: a {@code Link rel="next"} URI repeated. */
public final class PaginationLoopException extends PaginationException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** @param message what repeated or failed to advance */
    public PaginationLoopException(final String message) {
        super(message);
    }
}
