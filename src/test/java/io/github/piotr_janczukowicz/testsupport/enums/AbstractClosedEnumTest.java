package io.github.piotr_janczukowicz.testsupport.enums;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * The tests of a closed enum: every constant decodes from its wire value, and an unknown wire value is refused with an
 * {@link IllegalArgumentException}.
 *
 * @param <E> the enum
 * @param <W> its wire type
 */
public abstract class AbstractClosedEnumTest<E extends Enum<E>, W> extends AbstractEnumTest<E, W> {

    private final W unknown;

    /**
     * Creates the test of one closed enum.
     *
     * @param type the enum
     * @param of resolves a constant from its wire value
     * @param value gives the wire value of a constant
     * @param unknown a wire value no constant has
     */
    protected AbstractClosedEnumTest(
            final Class<E> type, final Function<W, E> of, final Function<E, W> value, final W unknown) {
        super(type, of, value, null);
        this.unknown = requireNonNull(unknown, "unknown");
    }

    @Test
    protected final void of_shouldRefuse_whenTheValueIsUnknown() {
        final Throwable thrown = catchThrowable(() -> resolve(unknown));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
