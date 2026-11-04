package io.github.piotr_janczukowicz.openapi.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a Java type to one or more catalog schemas. Each {@link #value()} entry is a catalog key
 * ({@code components/schemas/{name}} or {@code operations/{operationId}/{loc}}), not a JSON Pointer and not a dump
 * {@code $ref}.
 *
 * <p>A type that matches more than one catalog key lists them all in one annotation. Retention is
 * {@link RetentionPolicy#RUNTIME} so tests can discover annotated types without using dump locators.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CatalogSchema {

    /**
     * Catalog keys such as {@code components/schemas/simple-user} or {@code operations/pets/create/requestBody}. No
     * leading {@code /}.
     *
     * @return catalog keys
     */
    String[] value();
}
