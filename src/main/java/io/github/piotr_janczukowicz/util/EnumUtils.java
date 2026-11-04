package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum lookup helpers.
 *
 * <p>Resolves enum constants from external strings that may differ from {@link Enum#name()}. Lookups are exact string
 * matches against an immutable {@link WireIndex} that the caller owns, typically in a {@code static final} field of the
 * enum. The library keeps no global cache, so each index is bound to the mapping it was built with and to the lifetime
 * of its owner.
 */
public final class EnumUtils {

    private EnumUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds an immutable lookup from wire strings to the constants of {@code enumClass}.
     *
     * <p>{@code wireValue} is applied once to every constant, during this call. The result does not reference
     * {@code wireValue}. Hold the result in a {@code static final} field so the index is built once.
     *
     * @param enumClass enum type
     * @param wireValue mapping from constant to wire string
     * @param <E> enum type
     * @return the lookup
     * @throws NullPointerException if any argument is {@code null} or {@code wireValue} returns {@code null}
     * @throws IllegalStateException if two constants map to the same wire string
     */
    public static <E extends Enum<E>> WireIndex<E> index(
            final Class<E> enumClass, final Function<E, String> wireValue) {
        requireNonNull(enumClass, "enumClass");
        requireNonNull(wireValue, "wireValue");

        final Map<String, E> constants = Stream.of(enumClass.getEnumConstants())
                .collect(Collectors.toUnmodifiableMap(
                        constant -> requireNonNull(wireValue.apply(constant), "wire value of " + constant),
                        Function.identity()));
        return new WireIndex<>(constants);
    }

    /**
     * Immutable, thread-safe lookup from wire strings to enum constants. Matching is case-sensitive {@link String}
     * equality, not {@link Enum#valueOf(Class, String)}.
     *
     * @param <E> enum type
     */
    public static final class WireIndex<E extends Enum<E>> {

        private final Map<String, E> constants;

        private WireIndex(final Map<String, E> constants) {
            this.constants = constants;
        }

        /**
         * Returns the constant whose wire string equals {@code value}.
         *
         * @param value wire string to match
         * @return matching constant, or empty if none match
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public Optional<E> fromWire(final String value) {
            requireNonNull(value, "value");

            return Optional.ofNullable(constants.get(value));
        }

        /**
         * Returns the constant whose wire string equals {@code value}, or {@code defaultValue} when none match.
         *
         * @param value wire string to match
         * @param defaultValue constant returned when nothing matches
         * @return matching constant, or {@code defaultValue}
         * @throws NullPointerException if any argument is {@code null}
         */
        public E fromWire(final String value, final E defaultValue) {
            requireNonNull(defaultValue, "defaultValue");

            return fromWire(value).orElse(defaultValue);
        }
    }
}
