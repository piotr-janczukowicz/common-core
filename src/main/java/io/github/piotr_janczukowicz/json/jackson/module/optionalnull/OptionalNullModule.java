package io.github.piotr_janczukowicz.json.jackson.module.optionalnull;

import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.Serial;
import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson module that reads and writes {@link OptionalNull}: a Java {@code null} wrapper is left out by the enclosing
 * type's {@code NON_NULL} inclusion, {@link OptionalNull#ofNull()} is written as JSON {@code null}, and
 * {@link OptionalNull#of(Object)} is written as its value. Reading turns JSON {@code null} into
 * {@link OptionalNull#ofNull()}; an absent property stays Java {@code null}.
 *
 * <p>Registers {@link OptionalNullDeserializer} and {@link OptionalNullMixin}. This type does not hold data.
 */
public final class OptionalNullModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Version VERSION =
            new Version(1, 0, 0, null, "io.github.piotr-janczukowicz", "common-json-optionalnull");

    /** Registers the deserializer and the mixin for {@link OptionalNull}. */
    public OptionalNullModule() {
        super(VERSION);
        addDeserializer(OptionalNull.class, new OptionalNullDeserializer());
        setMixInAnnotation(OptionalNull.class, OptionalNullMixin.class);
    }
}
