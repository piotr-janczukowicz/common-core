package io.github.piotr_janczukowicz.json.jackson;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonEmpty;
import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/** Registers Jackson adapters that unwrap a sealed object union and pick a branch from a known JSON field. */
public final class PolymorphicObjectAdapters {

    private static final Logger LOG = LoggerFactory.getLogger(PolymorphicObjectAdapters.class);

    private PolymorphicObjectAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers a serializer that writes {@code unwrap}'s result in place of the union, and a deserializer that picks
     * the first branch whose field is present and not {@code null}.
     *
     * @param module module to add to
     * @param type sealed union type
     * @param unwrap extracts the object to write from a union value
     * @param unknownShape message of the failure when no branch field is present
     * @param branches branches in priority order; copied
     * @param <T> union type
     */
    public static <T> void register(
            final SimpleModule module,
            final Class<T> type,
            final Function<T, Object> unwrap,
            final String unknownShape,
            final List<Branch<T>> branches) {
        requireNonNull(module, "module");
        requireNonNull(type, "type");
        requireNonNull(unwrap, "unwrap");
        requireNonNull(unknownShape, "unknownShape");
        requireNonEmpty(branches, "branches");
        module.addSerializer(type, new UnwrapSerializer<>(type, unwrap));
        module.addDeserializer(type, new FieldBranchDeserializer<>(type, unknownShape, List.copyOf(branches)));
    }

    /**
     * Branch that reads {@code valueType} and wraps it into the union.
     *
     * @param field JSON property whose presence selects this branch
     * @param valueType type of the branch's object
     * @param wrap builds the union value from the decoded object
     * @param <T> union type
     * @param <V> branch value type
     * @return branch
     */
    public static <T, V> Branch<T> branch(final String field, final Class<V> valueType, final Function<V, T> wrap) {
        requireNonNull(field, "field");
        requireNonNull(valueType, "valueType");
        requireNonNull(wrap, "wrap");
        return new Branch<>(field, valueType, value -> wrap.apply(valueType.cast(value)));
    }

    /**
     * One alternative of a union. Create it with {@link #branch(String, Class, Function)}.
     *
     * @param <T> union type
     */
    public static final class Branch<T> {

        private final String field;
        private final Class<?> valueType;
        private final Function<Object, T> wrap;

        private Branch(final String field, final Class<?> valueType, final Function<Object, T> wrap) {
            this.field = field;
            this.valueType = valueType;
            this.wrap = wrap;
        }

        private T readFrom(final BufferedJson json) {
            return wrap.apply(json.read(valueType));
        }
    }

    private static final class UnwrapSerializer<T> extends StdSerializer<T> {

        private final Function<T, Object> unwrap;

        private UnwrapSerializer(final Class<T> type, final Function<T, Object> unwrap) {
            super(type);
            this.unwrap = unwrap;
        }

        @Override
        public void serialize(final T value, final JsonGenerator generator, final SerializationContext context) {
            context.writeValue(generator, unwrap.apply(value));
        }
    }

    private static final class FieldBranchDeserializer<T> extends StdDeserializer<T> {

        private final String unknownShape;
        private final List<Branch<T>> branches;
        private final List<String> fields;
        private final String typeName;

        private FieldBranchDeserializer(
                final Class<T> type, final String unknownShape, final List<Branch<T>> branches) {
            super(type);
            this.unknownShape = unknownShape;
            this.branches = branches;
            this.fields = branches.stream().map(branch -> branch.field).toList();
            this.typeName = type.getSimpleName();
        }

        @Override
        public T deserialize(final JsonParser parser, final DeserializationContext context) {
            try (BufferedJson json = BufferedJson.capture(parser, context)) {
                return read(json);
            }
        }

        private T read(final BufferedJson json) {
            final int index = json.firstNonNull(fields);
            if (index < 0) {
                LOG.atDebug()
                        .setMessage("No branch of {} matched: none of the fields {} is present and not null")
                        .addArgument(typeName)
                        .addArgument(fields)
                        .log();
                throw json.mismatch(unknownShape);
            }
            LOG.atDebug()
                    .setMessage("Reading {} through the branch selected by field {}")
                    .addArgument(typeName)
                    .addArgument(fields.get(index))
                    .log();
            return branches.get(index).readFrom(json);
        }
    }
}
