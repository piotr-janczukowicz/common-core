package io.github.piotr_janczukowicz.testsupport.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The tests of an open enum with a string wire value: those of {@link AbstractOpenEnumTest}, and a {@code null} wire
 * value is refused with a {@link NullPointerException} that names {@code value}.
 *
 * @param <E> the enum
 */
public abstract class AbstractOpenStringEnumTest<E extends Enum<E>> extends AbstractOpenEnumTest<E, String> {

    /**
     * Creates the test of one open string enum.
     *
     * @param type the enum
     * @param of resolves a constant from its wire value
     * @param value gives the wire value of a constant
     * @param unknown a wire value no constant has
     * @param unrecognized the constant an unknown wire value resolves to
     */
    protected AbstractOpenStringEnumTest(
            final Class<E> type,
            final Function<String, E> of,
            final Function<E, String> value,
            final String unknown,
            final E unrecognized) {
        super(type, of, value, unknown, unrecognized);
    }

    @Test
    protected final void of_shouldRefuseNull_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> resolve(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }
}
