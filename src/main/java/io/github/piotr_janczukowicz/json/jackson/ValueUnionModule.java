package io.github.piotr_janczukowicz.json.jackson;

import io.github.piotr_janczukowicz.json.ValueUnion;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.Deserializers;

/**
 * Jackson module that reads every class annotated with {@link ValueUnion}, so that a model needs no Jackson databind
 * type of its own. {@link JsonMappers#snakeCase()} adds it. Writing needs nothing: each branch writes itself.
 *
 * <p>The annotation is looked up on the class being read only, so a branch that implements a value union is read as
 * itself, not as the union.
 */
public final class ValueUnionModule extends JacksonModule {

    private static final Version VERSION =
            new Version(1, 0, 0, null, "io.github.piotr-janczukowicz", "common-json-value-union");

    @Override
    public String getModuleName() {
        return "ValueUnion";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule(final SetupContext context) {
        context.addDeserializers(new ValueUnionDeserializers());
    }

    static final class ValueUnionDeserializers extends Deserializers.Base {

        @Override
        public @Nullable ValueDeserializer<?> findBeanDeserializer(
                final JavaType type, final DeserializationConfig config, final BeanDescription.Supplier description) {
            final ValueUnion union = type.getRawClass().getAnnotation(ValueUnion.class);
            return union == null ? null : new ValueUnionDeserializer(type.getRawClass(), union);
        }

        @Override
        public boolean hasDeserializerFor(final DeserializationConfig config, final Class<?> valueType) {
            return valueType.isAnnotationPresent(ValueUnion.class);
        }
    }
}
