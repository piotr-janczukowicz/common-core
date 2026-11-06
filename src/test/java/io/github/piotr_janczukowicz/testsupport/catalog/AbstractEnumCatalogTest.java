package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.CatalogTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

@CatalogTest
public abstract class AbstractEnumCatalogTest {

    private static final Set<String> FALLBACK_CONSTANTS = Set.of("UNRECOGNIZED");
    private static final JsonMapper TREE = JsonMapper.builder().build();

    protected abstract Class<?> type();

    /**
     * The codec that decodes the fixtures and encodes the decoded values.
     *
     * @return the codec of the module under test
     */
    protected abstract JsonCodec codec();

    /**
     * The name of a stored schema file of the enum, used when {@link #schemaSource()} is {@code null}.
     *
     * @return the schema resource name, or {@code null} when the schemas come from {@link #schemaSource()}
     */
    protected String schema() {
        return null;
    }

    /**
     * Where the schema of every {@code @CatalogSchema} pointer of the enum comes from. When it is given, the checks run
     * against <em>every</em> pointer the source covers, and no stored schema file is needed.
     *
     * @return the source, or {@code null} to check the single stored file of {@link #schema()}
     */
    protected CatalogSchemaSource schemaSource() {
        return null;
    }

    @Test
    protected final void encode_shouldMatchOpenApiSchema_whenDecodedValueEncoded() {
        final Class<?> type = enumType();

        schemaJsons().forEach((label, schemaJson) -> {
            final Schema schema = CatalogChecks.compile(schemaJson);
            final List<JsonNode> items = dumpEnumItems(TREE.readTree(schemaJson));

            assertSoftly(softly -> {
                for (final JsonNode item : items) {
                    final Object decoded = requireNonNull(codec().decode(jsonBytes(item), type), "decoded");
                    final byte[] encoded = codec().encode(decoded);
                    final List<Error> errors =
                            schema.validate(new String(encoded, StandardCharsets.UTF_8), InputFormat.JSON);
                    softly.assertThat(errors).as(label + " " + item.asString()).isEmpty();
                }
            });
        });
    }

    @Test
    protected final void decode_shouldMapAllSchemaValues_whenOpenApiEnumDecoded() {
        final Class<?> type = enumType();
        final Map<String, String> schemas = schemaJsons();
        final Set<String> allDocsValues = new LinkedHashSet<>();
        boolean hasNull = false;

        for (final Map.Entry<String, String> entry : schemas.entrySet()) {
            final String label = entry.getKey();
            final JsonNode dumpSchema = TREE.readTree(entry.getValue());
            final List<String> docsValues = dumpEnumValues(dumpSchema);
            allDocsValues.addAll(docsValues);
            hasNull |= enumContainsNull(dumpEnumNode(dumpSchema));
            final Set<String> javaValues = javaWireValues(type, docsValues);

            assertSoftly(softly -> {
                for (final JsonNode item : dumpEnumItems(dumpSchema)) {
                    final String value = item.asString();
                    final Object decoded = requireNonNull(codec().decode(jsonBytes(item), type), value);
                    softly.assertThat(TREE.readTree(codec().encode(decoded)).asString())
                            .as(label + " " + value)
                            .isEqualTo(value);
                }
                softly.assertThat(javaValues).as(label).containsAll(docsValues);
            });
        }

        final List<String> union = List.copyOf(allDocsValues);
        final Set<String> javaValues = javaWireValues(type, union);
        // One pointer: the enum matches it exactly. Several: the enum is the union of what the pointers list.
        if (!hasNull) {
            assertThat(javaValues)
                    .as("wire values of " + type.getSimpleName())
                    .containsExactlyInAnyOrderElementsOf(union);
        }
    }

    private Class<?> enumType() {
        final Class<?> type = requireNonNull(type(), "type");
        assertThat(type.isEnum()).as(type.getTypeName()).isTrue();
        return type;
    }

    /**
     * The schema documents to check, keyed by what they came from: one per covered {@code @CatalogSchema} pointer, or
     * the stored file of {@link #schema()}.
     */
    private Map<String, String> schemaJsons() {
        final CatalogSchemaSource source = schemaSource();
        final Map<String, String> schemas = new LinkedHashMap<>();
        if (source == null) {
            final String schemaFile = requireNonNull(schema(), "schema or schemaSource");
            final byte[] bytes = requireNonNull(FileUtils.readBytes(getClass(), schemaFile), "schema");
            schemas.put(schemaFile, new String(bytes, StandardCharsets.UTF_8));
            return schemas;
        }
        final List<String> pointers = CatalogChecks.jsonBodyPointers(enumType(), source);
        assertThat(pointers)
                .as("@CatalogSchema pointers covered by the schema source")
                .isNotEmpty();
        for (final String pointer : pointers) {
            schemas.put(pointer, requireNonNull(source.schemaJson(pointer), "schema"));
        }
        return schemas;
    }

    private static JsonNode dumpEnumNode(final JsonNode dumpSchema) {
        final JsonNode enumNode = dumpSchema.get("enum");
        if (enumNode != null) {
            assertThat(enumNode.isArray()).isTrue();
            return enumNode;
        }
        return mergedBranchEnumNode(dumpSchema);
    }

    private static JsonNode mergedBranchEnumNode(final JsonNode dumpSchema) {
        final ArrayNode merged = TREE.createArrayNode();
        for (final String keyword : List.of("oneOf", "anyOf")) {
            final JsonNode branches = dumpSchema.get(keyword);
            if (branches == null) {
                continue;
            }
            for (final JsonNode branch : branches) {
                final JsonNode branchEnum = branch.get("enum");
                assertThat(branchEnum).as(keyword + " branch enum").isNotNull();
                assertThat(branchEnum.isArray()).isTrue();
                for (final JsonNode item : branchEnum) {
                    boolean present = false;
                    for (final JsonNode existing : merged) {
                        present |= existing.equals(item);
                    }
                    if (!present) {
                        merged.add(item);
                    }
                }
            }
        }
        assertThat(merged.isEmpty()).as("enum or oneOf/anyOf enum branches").isFalse();
        return merged;
    }

    private static List<JsonNode> dumpEnumItems(final JsonNode dumpSchema) {
        final List<JsonNode> items = wireEnumItems(dumpEnumNode(dumpSchema));
        assertThat(items).isNotEmpty();
        return items;
    }

    private static List<String> dumpEnumValues(final JsonNode dumpSchema) {
        final List<String> values = new ArrayList<>();
        for (final JsonNode item : dumpEnumItems(dumpSchema)) {
            values.add(item.asString());
        }
        return List.copyOf(values);
    }

    private Set<String> javaWireValues(final Class<?> type, final List<String> docsValues) {
        final Set<String> values = new LinkedHashSet<>();
        for (final Object constant : requireNonNull(type.getEnumConstants(), "constants")) {
            final String wire = TREE.readTree(codec().encode(constant)).asString();
            final boolean isAddedFallback =
                    FALLBACK_CONSTANTS.contains(((Enum<?>) constant).name()) && !docsValues.contains(wire);
            if (!isAddedFallback) {
                values.add(wire);
            }
        }
        return values;
    }

    private static List<JsonNode> wireEnumItems(final JsonNode enumNode) {
        final List<JsonNode> items = new ArrayList<>();
        for (final JsonNode item : enumNode) {
            if (item.isString() || item.isIntegralNumber()) {
                items.add(item);
            } else {
                assertThat(item.isNull())
                        .as("enum entry " + item + " must be a string, an integer or null")
                        .isTrue();
            }
        }
        return List.copyOf(items);
    }

    private static boolean enumContainsNull(final JsonNode enumNode) {
        for (final JsonNode item : enumNode) {
            if (item.isNull()) {
                return true;
            }
        }
        return false;
    }

    private static byte[] jsonBytes(final JsonNode item) {
        return TREE.writeValueAsBytes(item);
    }
}
