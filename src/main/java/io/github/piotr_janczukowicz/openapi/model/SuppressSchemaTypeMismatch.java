package io.github.piotr_janczukowicz.openapi.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Skips the docs-schema type check for this record component. Property name and {@code @Nullable} checks still run. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD})
public @interface SuppressSchemaTypeMismatch {

    /**
     * Why the declared Java type differs from the documented schema type.
     *
     * @return reason shown to maintainers; not interpreted by the check
     */
    String value();
}
