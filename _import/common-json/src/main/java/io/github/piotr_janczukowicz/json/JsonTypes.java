package io.github.piotr_janczukowicz.json;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Factories for {@link JsonType} values used at JSON decode sites. */
public final class JsonTypes {

    private static final int SINGLE_TYPE_PARAMETER = 1;

    private JsonTypes() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Token for the raw type {@code type}.
     *
     * @param type target class
     * @param <T> target type
     * @return token
     * @throws NullPointerException if {@code type} is {@code null}
     */
    public static <T> JsonType<T> of(final Class<T> type) {
        return new JsonType<>(requireNonNull(type, "type"));
    }

    /**
     * Token for a single-argument parameterized type such as {@code Wrapper<Item>}.
     *
     * <p>The compiler checks that {@code rawType} is the raw class of the result, so {@code JsonType<Map<Integer,
     * Integer>> type = of(List.class, String.class)} does not compile. It cannot check {@code typeArgument} against the
     * type argument of {@code R}: Java cannot derive {@code Wrapper<Item>} from the two classes, so a wrong argument
     * still fails with a {@link ClassCastException} where the decoded value is used. Assign the result to a constant of
     * the exact type, as the search API does, so the one place that spells the type is easy to check.
     *
     * @param rawType raw class of the result
     * @param typeArgument type argument
     * @param <R> resulting Java type, as the caller declares it
     * @return token
     * @throws NullPointerException if {@code rawType} or {@code typeArgument} is {@code null}
     * @throws IllegalArgumentException if {@code rawType} does not have exactly one type parameter
     */
    public static <R> JsonType<R> of(final Class<? super R> rawType, final Class<?> typeArgument) {
        requireNonNull(rawType, "rawType");
        requireNonNull(typeArgument, "typeArgument");
        final int parameterCount = rawType.getTypeParameters().length;
        if (parameterCount != SINGLE_TYPE_PARAMETER) {
            throw new IllegalArgumentException(
                    rawType.getName() + " has " + parameterCount + " type parameters, expected exactly 1");
        }
        return new JsonType<>(new Parameterized(rawType, new Type[] {typeArgument}));
    }

    /**
     * Token for {@code List} of {@code elementType}.
     *
     * @param elementType list element class
     * @param <T> element type
     * @return token
     * @throws NullPointerException if {@code elementType} is {@code null}
     */
    public static <T> JsonType<List<T>> listOf(final Class<T> elementType) {
        requireNonNull(elementType, "elementType");
        return new JsonType<>(new Parameterized(List.class, new Type[] {elementType}));
    }

    /**
     * Token for {@code Map} of {@link String} keys to {@code valueType}.
     *
     * @param valueType map value class
     * @param <V> value type
     * @return token
     * @throws NullPointerException if {@code valueType} is {@code null}
     */
    public static <V> JsonType<Map<String, V>> mapOf(final Class<V> valueType) {
        requireNonNull(valueType, "valueType");
        return new JsonType<>(new Parameterized(Map.class, new Type[] {String.class, valueType}));
    }

    private record Parameterized(Class<?> raw, Type[] arguments) implements ParameterizedType {

        private Parameterized {
            arguments = arguments.clone();
        }

        @Override
        public Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public @Nullable Type getOwnerType() {
            return null;
        }

        @Override
        public String getTypeName() {
            final StringBuilder text = new StringBuilder(raw.getTypeName()).append('<');
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(arguments[i].getTypeName());
            }
            return text.append('>').toString();
        }

        @Override
        public boolean equals(final @Nullable Object other) {
            return this == other
                    || other instanceof final ParameterizedType that
                            && raw.equals(that.getRawType())
                            && that.getOwnerType() == null
                            && Arrays.equals(arguments, that.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return raw.hashCode() ^ Arrays.hashCode(arguments);
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }
}
