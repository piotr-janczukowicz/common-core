package io.github.piotr_janczukowicz.json.jackson.module.immutable;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.core.Version;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson module that deserializes {@link List}, {@link Collection}, {@link Set}, and {@link Map} into unmodifiable
 * collections while keeping element types.
 *
 * <p>JSON {@code null} and absent properties stay {@code null}, so nullable fields keep their meaning. Null elements,
 * and map entries with a null value, are kept. Lists and maps keep the order of the JSON; the order of a set is
 * unspecified. This type does not hold collection data.
 */
public final class ImmutableCollectionsModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Version VERSION = new Version(1, 0, 0, null, "io.github.piotr-janczukowicz", "common-json");

    /**
     * Registers the modifier that makes {@link List}, {@link Collection}, {@link Set}, and {@link Map} deserializers
     * return immutable collections.
     *
     * <p>Does not mutate caller-owned collections.
     */
    public ImmutableCollectionsModule() {
        super(VERSION);
        setDeserializerModifier(new ImmutableCollectionsModifier());
    }
}
