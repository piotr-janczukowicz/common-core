package io.github.piotr_janczukowicz.http.uri;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNoNullElements;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *   <li>a map becomes one {@code name[key]=value} parameter per entry, and an entry whose value is {@code null} is left
 *       out;
 *   <li>a parameter name must not be blank.
 * </ul>
 *
 * <p>The builder is not thread-safe. The built map is unmodifiable, and the values are not encoded: encoding is left to
 * the request that sends them.
 */
public final class QueryParameters {

    private static final String NAME = "name";
    private static final String WIRE_VALUE = "wireValue";

    private final Map<String, String> query = new LinkedHashMap<>();

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
     * @param wireValue gives the wire value of a constant
     * @param <E> the enum type
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws NullPointerException if {@code wireValue} returns {@code null} for {@code value}
     */
    public <E> QueryParameters wire(final String name, final @Nullable E value, final Function<E, ?> wireValue) {
        requireNotBlank(name, NAME);
        requireNonNull(wireValue, WIRE_VALUE);
        if (value != null) {
            query.put(name, String.valueOf(requireNonNull(wireValue.apply(value), name + " wire value")));
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
     * @param wireValue gives the wire value of a constant
     * @param <E> the enum type
     * @return this builder
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     * @throws IllegalArgumentException if {@code values} holds {@code null}
     * @throws NullPointerException if {@code wireValue} returns {@code null} for an item
     */
    public <E> QueryParameters wireList(
            final String name, final @Nullable List<E> values, final Function<E, ?> wireValue) {
        requireNotBlank(name, NAME);
        requireNonNull(wireValue, WIRE_VALUE);
        if (values != null && !values.isEmpty()) {
            requireNoNullElements(values, name);
            query.put(
                    name,
                    values.stream()
                            .map(item -> requireNonNull(wireValue.apply(item), name + " wire value"))
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
     * The parameters, in the order they were added.
     *
     * @return an unmodifiable map
     */
    public Map<String, String> build() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(query));
    }
}
