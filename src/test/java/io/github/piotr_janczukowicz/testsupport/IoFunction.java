package io.github.piotr_janczukowicz.testsupport;

import java.io.IOException;

/**
 * A function that may fail with an {@link IOException}, for a request a test hands to a helper that owns the client.
 *
 * @param <A> the argument
 * @param <R> the result
 */
@FunctionalInterface
public interface IoFunction<A, R> {

    /**
     * Applies the function.
     *
     * @param argument the argument
     * @return the result
     * @throws IOException if the request fails
     */
    R apply(A argument) throws IOException;
}
