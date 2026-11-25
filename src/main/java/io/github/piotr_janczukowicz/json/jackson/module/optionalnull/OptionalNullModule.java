package io.github.piotr_janczukowicz.json.jackson.module.optionalnull;

import io.github.piotr_janczukowicz.json.OptionalNull;
import java.io.Serial;
import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson module that reads {@link OptionalNull}: JSON {@code null} becomes {@link OptionalNull#ofNull()}, any other
 * value becomes {@link OptionalNull#of(Object)}, and an absent property stays Java {@code null}. Writing needs no
 * module; {@link OptionalNull} carries its own annotations. This type does not hold data.
 */
public final class OptionalNullModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Version VERSION =
            new Version(1, 0, 0, null, "io.github.piotr-janczukowicz", "common-json-optionalnull");

    /** Registers the {@link OptionalNull} deserializer. */
    public OptionalNullModule() {
        super(VERSION);
        addDeserializer(OptionalNull.class, new OptionalNullDeserializer());
    }
}
