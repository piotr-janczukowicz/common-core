package io.github.piotr_janczukowicz.testsupport;

import java.util.Objects;

/**
 * Argument checks with the same message format as {@code io.github.piotr_janczukowicz.util.ValidationUtils}. This
 * module must not depend on common-core (its own tests would then form a cycle), so the check is repeated here.
 */
final class Preconditions {

    private Preconditions() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <T> T requireNonNull(final T value, final String fieldName) {
        return Objects.requireNonNull(value, () -> fieldName + " must not be null");
    }
}
