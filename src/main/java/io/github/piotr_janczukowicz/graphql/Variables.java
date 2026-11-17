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
 *   <li>a list is kept as a list, an empty one too, and a list that holds {@code null} is refused;
 *   <li>a mapping function must not return {@code null};
 *   <li>a name added again replaces the earlier value.
 * </ul>
 *
 * <p>The builder is not thread-safe. The built map is unmodifiable.
 */
public final class Variables {

    private static final String NAME = "name";
    private static final String MAPPER = "mapper";
    private static final String TO_VARIABLE = "toVariable";
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
     * Adds a value as it is: a string, a number, a boolean, or a list of them.
     *
     * @param name the variable name
     * @param value the value, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public Variables value(final String name, final @Nullable Object value) {
        requireNonNull(name, NAME);
        if (value != null) {
            entries.put(name, value);
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
     */
    public <T> Variables value(final String name, final @Nullable T value, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (value != null) {
            entries.put(name, requireNonNull(mapper.apply(value), name + VALUE));
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
     * @throws IllegalArgumentException if {@code values} holds {@code null}
     */
    public <T> Variables values(
            final String name, final @Nullable List<T> values, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (values != null) {
            entries.put(name, mapAll(name, values, mapper));
        }
        return this;
    }

    /**
     * Adds an input object as its fields.
     *
     * @param name the variable name
     * @param value the input object, or {@code null} to leave it out
     * @param toVariable gives the fields of the input object
     * @param <T> the input object type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code toVariable} is {@code null}, or {@code toVariable} returns
     *     {@code null}
     */
    public <T> Variables input(
            final String name,
            final @Nullable T value,
            final Function<? super T, ? extends Map<String, ?>> toVariable) {
        requireNonNull(toVariable, TO_VARIABLE);
        return value(name, value, toVariable);
    }

    /**
     * Adds a list of input objects, each as its fields.
     *
     * @param name the variable name
     * @param values the input objects, or {@code null} to leave the list out
     * @param toVariable gives the fields of an input object
     * @param <T> the input object type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code toVariable} is {@code null}, or {@code toVariable} returns
     *     {@code null}
     * @throws IllegalArgumentException if {@code values} holds {@code null}
     */
    public <T> Variables inputs(
            final String name,
            final @Nullable List<T> values,
            final Function<? super T, ? extends Map<String, ?>> toVariable) {
        requireNonNull(toVariable, TO_VARIABLE);
        return values(name, values, toVariable);
    }

    /**
     * Adds a value that can be cleared: JSON {@code null} for {@link OptionalNull#ofNull()}, else the wrapped value as
     * it is.
     *
     * @param name the variable name
     * @param value the wrapper, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public Variables clearable(final String name, final @Nullable OptionalNull<?> value) {
        requireNonNull(name, NAME);
        if (value != null) {
            entries.put(name, value.value());
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
     */
    public <T> Variables clearable(
            final String name, final @Nullable OptionalNull<T> value, final Function<? super T, ?> mapper) {
        requireNonNull(name, NAME);
        requireNonNull(mapper, MAPPER);
        if (value != null) {
            final @Nullable T wrapped = value.value();
            entries.put(name, wrapped == null ? null : requireNonNull(mapper.apply(wrapped), name + VALUE));
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

    private static <T> List<Object> mapAll(
            final String name, final List<T> values, final Function<? super T, ?> mapper) {
        return requireNoNullElements(values, name).stream()
                .<Object>map(item -> requireNonNull(mapper.apply(item), name + VALUE))
                .toList();
    }
}
