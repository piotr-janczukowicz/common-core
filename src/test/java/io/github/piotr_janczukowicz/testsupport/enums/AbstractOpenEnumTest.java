package io.github.piotr_janczukowicz.testsupport.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The tests of an open enum: every constant decodes from its wire value, and an unknown wire value decodes to the
 * fallback constant, which the round trip leaves out.
 *
 * @param <E> the enum
 * @param <W> its wire type
 */
public abstract class AbstractOpenEnumTest<E extends Enum<E>, W> extends AbstractEnumTest<E, W> {

    private final W unknown;
    private final E unrecognized;

    /**
     * Creates the test of one open enum.
     *
     * @param type the enum
     * @param of resolves a constant from its wire value
     * @param value gives the wire value of a constant
     * @param unknown a wire value no constant has
     * @param unrecognized the constant an unknown wire value resolves to
     */
    protected AbstractOpenEnumTest(
            final Class<E> type,
            final Function<W, E> of,
            final Function<E, W> value,
            final W unknown,
            final E unrecognized) {
        super(type, of, value, unrecognized);
        this.unknown = unknown;
        this.unrecognized = unrecognized;
    }

    @Test
    protected final void of_shouldReturnUnrecognized_whenTheValueIsUnknown() {
        final E decoded = resolve(unknown);

        assertThat(decoded).isEqualTo(unrecognized);
    }
}
