package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** List helpers: immutable copies of a list, which may be required or absent. */
public final class ListUtils {

    private ListUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Copies a required list into an unmodifiable list.
     *
     * @param values the list to copy
     * @param name name of {@code values}, used in the exception message
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}
     * @throws NullPointerException if {@code values} is {@code null}, or has a {@code null} element
     */
    public static <T> List<T> copyOf(final List<T> values, final String name) {
        return List.copyOf(requireNonNull(values, name));
    }

    /**
     * Copies {@code values} into an unmodifiable list, and keeps an absent list absent.
     *
     * <p>Use it where {@code null} means "not given" and an empty list means "given, and empty", such as an optional
     * argument of a request.
     *
     * @param values the list to copy, or {@code null}
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}, or {@code null} if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} element
     */
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    public static <T> @Nullable List<T> copyOrNull(@Nullable final List<T> values) {
        if (values == null) {
            return null;
        }
        return List.copyOf(values);
    }

    /**
     * Copies {@code values} into an unmodifiable list, and turns an absent list into an empty one.
     *
     * @param values the list to copy, or {@code null}
     * @param <T> element type
     * @return an unmodifiable copy of {@code values}, or an empty list if {@code values} is {@code null}
     * @throws NullPointerException if {@code values} has a {@code null} element
     */
    public static <T> List<T> copyOrEmpty(@Nullable final List<T> values) {
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    /**
     * Turns an absent list into an empty one, without copying a present one.
     *
     * @param values the list, or {@code null}
     * @param <T> element type
     * @return {@code values} itself, or an empty list if {@code values} is {@code null}
     */
    public static <T> List<T> orEmpty(@Nullable final List<T> values) {
        if (values == null) {
            return List.of();
        }
        return values;
    }
}
