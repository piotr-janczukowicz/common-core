package io.github.piotr_janczukowicz.testsupport;

import java.io.IOException;

/**
 * A consumer that may fail with an {@link IOException}, for a request a test hands to a helper that owns the client.
 *
 * @param <A> the argument
 */
@FunctionalInterface
public interface IoConsumer<A> {

    /**
     * Performs the operation.
     *
     * @param argument the argument
     * @throws IOException if the request fails
     */
    void accept(A argument) throws IOException;
}
