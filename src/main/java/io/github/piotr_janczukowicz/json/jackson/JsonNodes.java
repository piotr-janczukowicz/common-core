package io.github.piotr_janczukowicz.json.jackson;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/** Reads of a {@link JsonNode} tree that tolerate a missing or differently typed field. */
public final class JsonNodes {

    private JsonNodes() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads a string field of an object node.
     *
     * @param node object node to read from
     * @param field name of the field
     * @return the text of the field, or {@code null} if the field is missing, {@code null}, or not a JSON string
     * @throws NullPointerException if {@code node} is {@code null}
     */
    public static @Nullable String textOrNull(final JsonNode node, final String field) {
        final JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            return null;
        }
        return value.asString();
    }
}
