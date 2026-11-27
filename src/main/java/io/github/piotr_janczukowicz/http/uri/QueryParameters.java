package io.github.piotr_janczukowicz.http.uri;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNoNullElements;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Builds the query parameters of a request, in the order they are added. The rules live here so that every caller, such
 * as a generated filter, states each parameter in one expression:
 *
 * <ul>
 *   <li>a value that is {@code null} is left out;
 *   <li>a list is joined with commas, an empty list is left out, and a list that holds {@code null} is refused;
 *   <li>a repeated list becomes one value per item, sent as {@code name=a&name=b}: an item that is {@code null} is left
 *       out, and a list that is empty or holds only {@code null} is left out;
 *   <li>a map becomes one {@code name[key]=value} parameter per entry, and an entry whose value is {@code null} is left
 *       out;
 *   <li>a parameter name must not be blank;
 *   <li>a name added again replaces the earlier value.
 * </ul>
 *
 * <p>{@link #buildValues()} gives every parameter as a list of values, for a request that sends a repeated parameter
 * once per value. {@link #build()} gives one value per name, and refuses a repeated parameter that holds more than one
 * value rather than join it.
 *
 * <p>The builder is not thread-safe. The built maps are unmodifiable, and the values are not encoded: encoding is left
 * to the request that sends them.
 */
public final class QueryParameters {

    private static final String NAME = "name";
    private static final String TO_VALUE = "toValue";
    private static final int SINGLE_VALUE = 1;

    // a String per single parameter, an unmodifiable List<String> per repeated one, in the order they were added
    private final Map<String, Object> query = new LinkedHashMap<>();
    // set once a repeated parameter is added: until then build() is one copy of the map, as for a builder without them
    private boolean hasRepeated;

    private QueryParameters() {}

    /**
     * Starts an empty set of parameters.
     *
     * @return the builder
     */
    public static QueryParameters builder() {
        return new QueryParameters();
    }

    /**
     * Adds every entry of {@code parameters}.
     *
     * @param parameters the entries to add, under the rules of {@link #text(String, String)}: an entry of the same name
     *     is replaced, and one whose value is {@code null} is left out
     * @return this builder
     * @throws NullPointerException if {@code parameters} is {@code null}, or holds a {@code null} name
     * @throws IllegalArgumentException if {@code parameters} holds a blank name
     */
    public QueryParameters all(final Map<String, String> parameters) {
        requireNonNull(parameters, "parameters").forEach(this::text);
        return this;
    }

    /**
     * Adds a text value.
     *
     * @param name the parameter name
     * @param value the value, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public QueryParameters text(final String name, final @Nullable String value) {
        requireNotBlank(name, NAME);
        if (value != null) {
            query.put(name, value);
        }
        return this;
    }

    /**
     * Adds a number, a boolean, a date or an instant with its {@code String.valueOf}.
     *
     * @param name the parameter name
     * @param value the value, or {@code null} to leave it out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public QueryParameters value(final String name, final @Nullable Object value) {
        requireNotBlank(name, NAME);
        if (value != null) {
            query.put(name, String.valueOf(value));
        }
        return this;
    }

    /**
     * Adds an enum by its wire value.
     *
     * @param name the parameter name
     * @param value the constant, or {@code null} to leave it out
     * @param toValue gives the value of a constant
     * @param <E> the enum type
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code toValue} returns {@code null} for {@code value}
     */
    public <E> QueryParameters wire(final String name, final @Nullable E value, final Function<E, ?> toValue) {
        requireNotBlank(name, NAME);
        requireNonNull(toValue, TO_VALUE);
        if (value != null) {
            query.put(name, String.valueOf(requireNonNull(toValue.apply(value), name + " wire value")));
        }
        return this;
    }

    /**
     * Adds a list, its items joined with commas.
     *
     * @param name the parameter name
     * @param values the items, or {@code null} to leave the parameter out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IllegalArgumentException if {@code values} holds {@code null}
     */
    public QueryParameters list(final String name, final @Nullable List<?> values) {
        requireNotBlank(name, NAME);
        if (values != null && !values.isEmpty()) {
            requireNoNullElements(values, name);
            query.put(name, values.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        return this;
    }

    /**
     * Adds a list of enum constants, their wire values joined with commas.
     *
     * @param name the parameter name
     * @param values the constants, or {@code null} to leave the parameter out
     * @param toValue gives the value of a constant
     * @param <E> the enum type
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IllegalArgumentException if {@code values} holds {@code null}
     * @throws NullPointerException if {@code toValue} returns {@code null} for an item
     */
    public <E> QueryParameters wireList(
            final String name, final @Nullable List<E> values, final Function<E, ?> toValue) {
        requireNotBlank(name, NAME);
        requireNonNull(toValue, TO_VALUE);
        if (values != null && !values.isEmpty()) {
            requireNoNullElements(values, name);
            query.put(
                    name,
                    values.stream()
                            .map(item -> requireNonNull(toValue.apply(item), name + " wire value"))
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));
        }
        return this;
    }

    /**
     * Adds a hash as {@code name[key]=value}, one parameter per entry.
     *
     * @param name the parameter name
     * @param values the entries, or {@code null} to leave the parameter out; an entry whose value is {@code null} is
     *     left out
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if a key is {@code null}
     */
    public QueryParameters map(final String name, final @Nullable Map<String, ?> values) {
        requireNotBlank(name, NAME);
        if (values != null) {
            values.forEach((key, item) -> {
                requireNonNull(key, name + " key");
                if (item != null) {
                    query.put(name + "[" + key + "]", String.valueOf(item));
                }
            });
        }
        return this;
    }

    /**
     * Adds a list sent once per item, as {@code name=a&name=b}, each item with its {@code String.valueOf}. Only
     * {@link #buildValues()} keeps more than one value.
     *
     * @param name the parameter name
     * @param values the items, or {@code null} to leave the parameter out; an item that is {@code null} is left out,
     *     and the parameter is left out when no item is left
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public QueryParameters repeated(final String name, final @Nullable List<?> values) {
        requireNotBlank(name, NAME);
        if (values != null) {
            putRepeated(
                    name,
                    values.stream()
                            .filter(Objects::nonNull)
                            .map(String::valueOf)
                            .toList());
        }
        return this;
    }

    /**
     * Adds a list of enum constants sent once per item, as {@code name=a&name=b}, each by its wire value. Only
     * {@link #buildValues()} keeps more than one value.
     *
     * @param name the parameter name
     * @param values the constants, or {@code null} to leave the parameter out; an item that is {@code null} is left
     *     out, and the parameter is left out when no item is left
     * @param toValue gives the value of a constant
     * @param <E> the enum type
     * @return this builder
     * @throws NullPointerException if {@code name} or {@code toValue} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code toValue} returns {@code null} for an item
     */
    public <E> QueryParameters wireRepeated(
            final String name, final @Nullable List<E> values, final Function<E, ?> toValue) {
        requireNotBlank(name, NAME);
        requireNonNull(toValue, TO_VALUE);
        if (values != null) {
            putRepeated(
                    name,
                    values.stream()
                            .filter(Objects::nonNull)
                            .map(item -> wired(name, item, toValue))
                            .toList());
        }
        return this;
    }

    /**
     * The parameters, one value per name, in the order they were added.
     *
     * @return an unmodifiable map
     * @throws IllegalStateException if a parameter added with {@link #repeated(String, List)} or
     *     {@link #wireRepeated(String, List, Function)} holds more than one value: its values are not joined, so the
     *     request must take {@link #buildValues()}
     */
    public Map<String, String> build() {
        if (!hasRepeated) {
            // every value is a String until a repeated parameter is added
            @SuppressWarnings("unchecked")
            final Map<String, String> single = (Map<String, String>) (Map<String, ?>) query;
            return Collections.unmodifiableMap(new LinkedHashMap<>(single));
        }
        final Map<String, String> single = new LinkedHashMap<>();
        query.forEach((name, value) -> single.put(name, oneValue(name, value)));
        return Collections.unmodifiableMap(single);
    }

    /**
     * Every parameter as its list of values, in the order they were added: a repeated parameter holds one value per
     * item, every other parameter holds one value.
     *
     * @return an unmodifiable map of unmodifiable lists
     */
    public Map<String, List<String>> buildValues() {
        final Map<String, List<String>> values = new LinkedHashMap<>();
        query.forEach((name, value) -> values.put(name, values(value)));
        return Collections.unmodifiableMap(values);
    }

    private void putRepeated(final String name, final List<String> values) {
        if (!values.isEmpty()) {
            query.put(name, values);
            hasRepeated = true;
        }
    }

    private static String oneValue(final String name, final Object value) {
        if (value instanceof String text) {
            return text;
        }
        final List<String> values = values(value);
        if (values.size() > SINGLE_VALUE) {
            throw new IllegalStateException("query parameter " + name + " holds " + values.size()
                    + " values; take buildValues() to send it once per value");
        }
        return values.getFirst();
    }

    @SuppressWarnings("unchecked")
    private static List<String> values(final Object value) {
        // a repeated parameter is stored as an unmodifiable List<String> (putRepeated), any other as a String
        return value instanceof String text ? List.of(text) : (List<String>) value;
    }

    private static <E> String wired(final String name, final E value, final Function<E, ?> toValue) {
        return String.valueOf(requireNonNull(toValue.apply(value), name + " wire value"));
    }
}
