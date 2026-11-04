package io.github.piotr_janczukowicz.openapi.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a collapsed simple {@code oneOf}/{@code anyOf} of JSON scalars and/or a string array.
 *
 * <p>{@link #value()} is dump branch {@code type}s joined by {@code |}, in branch order, without {@code null}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
public @interface CatalogSimpleXor {

    /**
     * Dump composition kinds, for example {@code string|integer} or {@code string|number|array}.
     *
     * @return kinds joined by {@code |}
     */
    String value();
}
