package io.github.piotr_janczukowicz.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/**
 * Tags the mapping test base: tests of how a type is read from and written as JSON. They are unit tests as far as Maven
 * is concerned, so the build that runs the {@code unit} tag runs them.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("unit")
public @interface MappingTest {}
