package io.github.piotr_janczukowicz.testsupport.catalog;

/** Tells where the schema of a {@code @CatalogSchema} pointer comes from, for the module under test. */
public interface CatalogSchemaSource {

    /**
     * Whether this source has a JSON body schema for the pointer.
     *
     * @param pointer a {@code @CatalogSchema} value
     * @return true when {@link #schemaJson(String)} can answer it
     */
    boolean covers(String pointer);

    /**
     * The merged, fully inlined JSON schema of a covered pointer.
     *
     * @param pointer a {@code @CatalogSchema} value for which {@link #covers(String)} is true
     * @return the schema document
     */
    String schemaJson(String pointer);
}
