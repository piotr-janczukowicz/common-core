package io.github.piotr_janczukowicz.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property whose JSON value may be an object with no properties standing for "no value": such an object and
 * JSON {@code null} both read as {@code null}; any other value reads as the property's declared type, refused as that
 * type refuses it. Writing is unchanged: a {@code null} is written as the property's usual null. The Jackson
 * {@code ValueUnionModule} reads it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface EmptyObjectAsNull {}
