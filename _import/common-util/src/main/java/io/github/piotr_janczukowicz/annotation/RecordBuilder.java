package io.github.piotr_janczukowicz.annotation;

import io.soabase.recordbuilder.core.RecordBuilder.Options;
import io.soabase.recordbuilder.core.RecordBuilder.Template;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a type-safe builder for an annotated record. Applied at compile time via record-builder; generated classes
 * are named {@code RecordNameBuilder} and annotated with {@code io.soabase.recordbuilder.core.RecordBuilderGenerated}
 * (CLASS retention) so JaCoCo and NullAway can skip them without explicit name-pattern excludes.
 */
@Template(options = @Options(enableWither = false, addClassRetainedGenerated = true))
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RecordBuilder {}
