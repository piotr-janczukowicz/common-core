package io.github.piotr_janczukowicz.testsupport.enums;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The round-trip test every wire enum shares: each constant decodes from its own wire value.
 *
 * @param <E> the enum
 * @param <W> its wire type
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractEnumTest<E extends Enum<E>, W> {

    private final Class<E> type;
    private final Function<W, E> of;
    private final Function<E, W> value;
    private final @Nullable E fallback;

    /**
     * Creates the test of one enum.
     *
     * @param type the enum
     * @param of resolves a constant from its wire value
     * @param value gives the wire value of a constant
     * @param fallback the constant an unknown wire value resolves to, which the round trip leaves out, or {@code null}
     *     for an enum that refuses an unknown wire value
     */
    AbstractEnumTest(
            final Class<E> type, final Function<W, E> of, final Function<E, W> value, @Nullable final E fallback) {
        this.type = requireNonNull(type, "type");
        this.of = requireNonNull(of, "of");
        this.value = requireNonNull(value, "value");
        this.fallback = fallback;
    }

    /**
     * Resolves a constant from a wire value.
     *
     * @param wire the wire value
     * @return the constant {@code of} gives
     */
    final E resolve(final @Nullable W wire) {
        return of.apply(wire);
    }

    @ParameterizedTest
    @MethodSource("constants")
    protected final void of_shouldReturnTheConstant_whenTheWireValueIsGiven(final E constant) {
        final E decoded = of.apply(value.apply(constant));

        assertThat(decoded).isEqualTo(constant);
    }

    final Stream<E> constants() {
        return Arrays.stream(type.getEnumConstants()).filter(constant -> constant != fallback);
    }
}
