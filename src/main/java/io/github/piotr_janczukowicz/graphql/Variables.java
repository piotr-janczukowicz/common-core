package io.github.piotr_janczukowicz.graphql;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNoNullElements;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.json.OptionalNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Builds the variables of a GraphQL request, or the fields of one input object, in the order they are added. Unlike
 * query parameters, the values keep their JSON types: numbers, booleans, lists and nested input objects stay what they
 * are, so the JSON mapper that sends them writes them as such. The rules:
 *
 * <ul>
 *   <li>a value that is {@code null} is left out;
 *   <li>JSON {@code null} is written only for {@link OptionalNull#ofNull()}, through {@code clearable};
 *   <li>{@code value(name, value)} stores a list as an unmodifiable copy and {@code values} stores an unmodifiable list
 *       of the mapped items; an empty list is kept, and a list that holds {@code null} is refused;
 *   <li>other values, and what a mapping function returns, are stored as they are;
 *   <li>a mapping function must not return {@code null};
 *   <li>a name that was already added is refused; a value left out does not count as added.
 * </ul>
 *
 * <p>An input object is added through {@code value} or {@code values} with a mapping function that gives its fields,
 * such as {@code value("input", input, CreateInput::toVariable)}.
 *
 * <p>The builder is not thread-safe. The built map is unmodifiable.
 */
public final class Variables {

    private static final String NAME = "name";
    private static final String MAPPER = "mapper";
    private static final String VALUE = " value";

    private final Map<String, @Nullable Object> entries = new LinkedHashMap<>();

    private Variables() {}

    /**
     * Starts an empty set of variables.
     *
     * @return the builder
     */
    public static Variables builder() {
        return new Variables();
    }

    /**
     * Adds a value as it is: a string, a number, a boolean, or a list of them. A list is stored as an unmodifiable copy.
     *
     * @param name the variable name
     * @param value the value, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} was already added, or {@code value} is a list that holds
     *     {@code null}
     */
    public Variables value(final String name, final @Nullable Object value) {
        requireNonNull(name, NAME);
        if (value != null) {
            put(name, value instanceof List<?> list ? List.copyOf(requireNoNullElements(list, name)) : value);
        }
        return this;
    }

    /**
     * Adds a value in its wire form, such as an enum by its wire value or a date as text.
     *
     * @param name the variable name
     * @param value the value, or {@code null} to leave it out
     * @param mapper gives the wire form of the value
     * @param <T> the value type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code mapper} is {@code null}, or {@code mapper} returns
     *     {@code null}
     * @throws IllegalArgumentException if {@code name} was already added
     */
    public <T> Variables value(final String name, final @Nullable T value, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (value != null) {
            put(name, requireNonNull(mapper.apply(value), name + VALUE));
        }
        return this;
    }

    /**
     * Adds a list, each item in its wire form, such as enums by their wire values.
     *
     * @param name the variable name
     * @param values the items, or {@code null} to leave the list out
     * @param mapper gives the wire form of an item
     * @param <T> the item type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code mapper} is {@code null}, or {@code mapper} returns
     *     {@code null}
     * @throws IllegalArgumentException if {@code name} was already added, or {@code values} holds {@code null}
     */
    public <T> Variables values(
            final String name, final @Nullable List<T> values, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (values != null) {
            put(name, mapAll(name, values, mapper));
        }
        return this;
    }

    /**
     * Adds a value that can be cleared: JSON {@code null} for {@link OptionalNull#ofNull()}, else the wrapped value as
     * it is.
     *
     * @param name the variable name
     * @param value the wrapper, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} was already added
     */
    public Variables clearable(final String name, final @Nullable OptionalNull<?> value) {
        requireNonNull(name, NAME);
        if (value != null) {
            put(name, value.value());
        }
        return this;
    }

    /**
     * Adds a value that can be cleared: JSON {@code null} for {@link OptionalNull#ofNull()}, else the wrapped value in
     * its wire form.
     *
     * @param name the variable name
     * @param value the wrapper, or {@code null} to leave it out
     * @param mapper gives the wire form of the wrapped value
     * @param <T> the value type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code mapper} is {@code null}, or {@code mapper} returns
     *     {@code null}
     * @throws IllegalArgumentException if {@code name} was already added
     */
    public <T> Variables clearable(
            final String name, final @Nullable OptionalNull<T> value, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (value != null) {
            final T wrapped = value.value();
            put(name, wrapped == null ? null : requireNonNull(mapper.apply(wrapped), name + VALUE));
        }
        return this;
    }

    /**
     * The variables, in the order they were added.
     *
     * @return an unmodifiable map; a value is {@code null} only where {@code clearable} wrote JSON {@code null}
     */
    public Map<String, @Nullable Object> build() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    private void put(final String name, final @Nullable Object value) {
        if (entries.containsKey(name)) {
            throw new IllegalArgumentException("Variable '" + name + "' was already added");
        }
        entries.put(name, value);
    }

    private static <T> List<Object> mapAll(
            final String name, final List<T> values, final Function<? super T, ?> mapper) {
        return requireNoNullElements(values, name).stream()
                .<Object>map(item -> requireNonNull(mapper.apply(item), name + VALUE))
                .toList();
    }
}
