package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Constructor;

final class UtilityClassAssertions {

    private UtilityClassAssertions() {
        throw new UnsupportedOperationException("Test helper");
    }

    /**
     * Asserts that the private no-arg constructor of {@code utilityClass} rejects instantiation.
     *
     * <p>Reflection is the only way to reach a private constructor, so the accessibility override is intentional.
     */
    static void assertNotInstantiable(final Class<?> utilityClass) throws ReflectiveOperationException {
        final Constructor<?> constructor = utilityClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
