package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import io.github.piotr_janczukowicz.json.EmptyObjectAsNull;
import io.github.piotr_janczukowicz.json.EpochInstant;
import io.github.piotr_janczukowicz.json.PresenceUnion;
import io.github.piotr_janczukowicz.json.ValueUnion;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.introspect.Annotated;

/**
 * Jackson module that reads every class annotated with {@link ValueUnion} (picked by the first token) or
 * {@link PresenceUnion} (picked by the names of the object), and every property marked {@link EmptyObjectAsNull} or
 * {@link EpochInstant}, so that a model needs no Jackson databind type of its own. {@link JsonMappers#snakeCase()} adds
 * it. Writing needs nothing for unions: each branch writes itself; a property marked {@link EpochInstant} is also
 * written, as an integer count of its unit.
 *
 * <p>The annotation is looked up on the class being read only, so a branch that implements a union is read as itself,
 * not as the union.
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
        context.insertAnnotationIntrospector(new EmptyObjectAsNullIntrospector());
        context.insertAnnotationIntrospector(new EpochInstantIntrospector());
    }

    static final class EmptyObjectAsNullIntrospector extends AnnotationIntrospector {

        private static final long serialVersionUID = 1L;

        @Override
        public Version version() {
            return VERSION;
        }

        @Override
        public @Nullable Object findDeserializer(final MapperConfig<?> config, final Annotated annotated) {
            return annotated.hasAnnotation(EmptyObjectAsNull.class) ? new EmptyObjectAsNullDeserializer() : null;
        }
    }

    static final class EpochInstantIntrospector extends AnnotationIntrospector {

        private static final long serialVersionUID = 1L;

        @Override
        public Version version() {
            return VERSION;
        }

        @Override
        public @Nullable Object findDeserializer(final MapperConfig<?> config, final Annotated annotated) {
            final EpochInstant epoch = annotated.getAnnotation(EpochInstant.class);
            return epoch == null ? null : new EpochInstantDeserializer(epoch.value());
        }

        @Override
        public @Nullable Object findSerializer(final MapperConfig<?> config, final Annotated annotated) {
            final EpochInstant epoch = annotated.getAnnotation(EpochInstant.class);
            return epoch == null ? null : new EpochInstantSerializer(epoch.value());
        }
    }

    static final class ValueUnionDeserializers extends Deserializers.Base {

        @Override
        public @Nullable ValueDeserializer<?> findBeanDeserializer(
                final JavaType type, final DeserializationConfig config, final BeanDescription.Supplier description) {
            final Class<?> raw = type.getRawClass();
            final ValueUnion union = raw.getAnnotation(ValueUnion.class);
            if (union != null) {
                return new ValueUnionDeserializer(raw, union);
            }
            final PresenceUnion presence = raw.getAnnotation(PresenceUnion.class);
            return presence == null ? null : new PresenceUnionDeserializer(raw, presence);
        }

        @Override
        public boolean hasDeserializerFor(final DeserializationConfig config, final Class<?> valueType) {
            return valueType.isAnnotationPresent(ValueUnion.class)
                    || valueType.isAnnotationPresent(PresenceUnion.class);
        }
    }
}
