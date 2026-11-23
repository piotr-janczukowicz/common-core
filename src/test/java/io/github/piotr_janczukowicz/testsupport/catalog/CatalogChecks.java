package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import io.github.piotr_janczukowicz.openapi.model.SuppressSchemaTypeMismatch;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Checks shared by the catalog test bases. */
public final class CatalogChecks {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    private CatalogChecks() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * The {@code @CatalogSchema} pointers of a type that the source has a JSON body schema for.
     *
     * @param type the type under test
     * @param source tells which pointers it covers
     * @return the covered pointers, in annotation order
     */
    public static List<String> jsonBodyPointers(final Class<?> type, final CatalogSchemaSource source) {
        final CatalogSchema schema = type.getAnnotation(CatalogSchema.class);
        return schema == null
                ? List.of()
                : Arrays.stream(schema.value()).filter(source::covers).toList();
    }

    /**
     * Compiles an OpenAPI 3.1 schema.
     *
     * @param schemaJson the schema document
     * @return the compiled schema
     */
    public static Schema compile(final String schemaJson) {
        final Schema schema =
                SchemaRegistry.withDialect(Dialects.getOpenApi31()).getSchema(schemaJson, InputFormat.JSON);
        schema.initializeValidators();
        return schema;
    }

    /**
     * The schema of a pointer, ready to validate the whole encoded value of a type.
     *
     * <p>A top-level record component with {@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)} is a union picked by a
     * sibling value, and the spec's {@code oneOf} or {@code anyOf} of its branches may overlap, so "exactly one" says
     * nothing about our code. That one keyword is removed from the property's node (the property is looked up in the
     * root and in the root's {@code allOf} entries, through a nullable {@code anyOf}/{@code oneOf} of {@code null} and
     * one schema); its other keywords stay, so the other properties and the sibling's enum are still checked. The
     * branch itself is checked by {@link #selectedBranchFailures}. Unions with class-level type info stay strict.
     *
     * @param type the type under test
     * @param schemaJson the schema of one pointer
     * @return the schema unchanged when the type has no such component, else the relaxed copy
     * @throws AssertionError when the schema has no property node for such a component
     */
    public static String holderSchemaJson(final Class<?> type, final String schemaJson) {
        final List<RecordComponent> externals = externalComponents(type);
        if (externals.isEmpty()) {
            return schemaJson;
        }
        final JsonNode root = requireNonNull(TREE.readTree(schemaJson), "schema");
        for (final RecordComponent component : externals) {
            final String name = JavaDocsSchemaComparator.jsonName(component);
            final ObjectNode node = propertyNode(root, name);
            assertThat(node)
                    .as("property %s of the holder schema (EXTERNAL_PROPERTY component of %s)", name, type.getName())
                    .isNotNull();
            relax(node);
        }
        return TREE.writeValueAsString(root);
    }

    /**
     * Checks that each top-level {@code EXTERNAL_PROPERTY} component of the decoded value fits the branch Jackson
     * picked: its encoded value must validate against every covered {@code @CatalogSchema} pointer of the runtime class
     * of the decoded value. A record whose single component is {@code @JsonValue} is replaced by that value first. A
     * class with no covered pointer, such as an Unknown, is a failure.
     *
     * @param type the type under test
     * @param decoded the decoded fixture
     * @param encoded the JSON the codec wrote for {@code decoded}
     * @param source gives the schema of a pointer
     * @return the failures, empty when every component fits its branch
     */
    public static List<String> selectedBranchFailures(
            final Class<?> type, final Object decoded, final String encoded, final CatalogSchemaSource source) {
        final List<String> failures = new ArrayList<>();
        final JsonNode tree = requireNonNull(TREE.readTree(encoded), "encoded");
        for (final RecordComponent component : externalComponents(type)) {
            final String name = JavaDocsSchemaComparator.jsonName(component);
            Object value = invoke(component.getAccessor(), decoded);
            if (value == null) {
                continue;
            }
            final Class<?> decodedClass = value.getClass();
            while (value != null && jsonValueMethod(value.getClass()) != null) {
                value = invoke(jsonValueMethod(value.getClass()), value);
            }
            final JsonNode subtree = tree.get(name);
            if (value == null || subtree == null) {
                failures.add(name + ": nothing to validate, the value or the encoded property is missing");
                continue;
            }
            final Class<?> branch = value.getClass();
            final List<String> pointers = jsonBodyPointers(branch, source);
            if (pointers.isEmpty()) {
                failures.add(name + ": " + decodedClass.getName()
                        + (branch == decodedClass ? "" : " (wrapping " + branch.getName() + ")")
                        + " has no covered @CatalogSchema pointer");
                continue;
            }
            final String json = TREE.writeValueAsString(subtree);
            for (final String pointer : pointers) {
                compile(requireNonNull(source.schemaJson(pointer), "schema"))
                        .validate(json, InputFormat.JSON)
                        .forEach(error ->
                                failures.add(name + " as " + branch.getSimpleName() + " " + pointer + ": " + error));
            }
        }
        return failures;
    }

    private static List<RecordComponent> externalComponents(final Class<?> type) {
        if (!type.isRecord()) {
            return List.of();
        }
        return Arrays.stream(type.getRecordComponents())
                .filter(component -> isExternal(component.getAnnotation(JsonTypeInfo.class))
                        || isExternal(component.getAccessor().getAnnotation(JsonTypeInfo.class)))
                .toList();
    }

    private static boolean isExternal(final @Nullable JsonTypeInfo info) {
        return info != null && info.include() == JsonTypeInfo.As.EXTERNAL_PROPERTY;
    }

    private static @Nullable ObjectNode propertyNode(final JsonNode root, final String name) {
        if (root.path("properties").get(name) instanceof ObjectNode node) {
            return node;
        }
        for (final JsonNode part : root.path("allOf")) {
            if (part.path("properties").get(name) instanceof ObjectNode node) {
                return node;
            }
        }
        return null;
    }

    private static void relax(final ObjectNode node) {
        ObjectNode target = node;
        for (final String keyword : List.of("anyOf", "oneOf")) {
            final JsonNode parts = node.get(keyword);
            if (parts != null && parts.size() == 2) {
                final boolean firstNull = isNullSchema(parts.get(0));
                final boolean secondNull = isNullSchema(parts.get(1));
                if (firstNull != secondNull && parts.get(firstNull ? 1 : 0) instanceof ObjectNode other) {
                    target = other;
                    break;
                }
            }
        }
        target.remove("oneOf");
        target.remove("anyOf");
    }

    private static boolean isNullSchema(final JsonNode schema) {
        return schema.size() == 1 && "null".equals(schema.path("type").asString());
    }

    private static @Nullable Method jsonValueMethod(final Class<?> type) {
        if (!type.isRecord() || type.getRecordComponents().length != 1) {
            return null;
        }
        for (final Method method : type.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.isAnnotationPresent(JsonValue.class)) {
                return method;
            }
        }
        return null;
    }

    private static @Nullable Object invoke(final Method method, final Object target) {
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read " + method, e);
        }
    }

    /**
     * Differences between a type and a stored schema file.
     *
     * @param owner the class whose package holds the schema file
     * @param type the type under test
     * @param schemaFile the schema resource name
     * @return the differences; empty when the type matches the schema
     */
    public static List<String> docsSchemaMismatches(
            final Class<?> owner, final Class<?> type, final String schemaFile) {
        final byte[] schemaJson = requireNonNull(FileUtils.readBytes(owner, schemaFile), "schema");
        final JsonNode schema = requireNonNull(TREE.readTree(schemaJson), "schema");
        return JavaDocsSchemaComparator.diff(type, schema);
    }

    /**
     * Differences between a type and its docs schema, read from the pointers or from a stored file.
     *
     * <p>When {@code comparesEveryPointer} is set, the type must have at least one covered pointer and the result is
     * that of {@link #docsSchemaMismatches(Class, List, CatalogSchemaSource)}; otherwise it is that of
     * {@link #docsSchemaMismatches(Class, Class, String)}.
     *
     * @param owner the class whose package holds the schema file
     * @param type the type under test
     * @param comparesEveryPointer whether the pointers, not the stored file, are the reference
     * @param schemaFile the schema resource name; read only when {@code comparesEveryPointer} is {@code false}
     * @param source gives the schema of a pointer
     * @return the differences; empty when the type matches
     * @throws AssertionError when every pointer is compared and the type has no covered pointer
     */
    public static List<String> docsSchemaMismatches(
            final Class<?> owner,
            final Class<?> type,
            final boolean comparesEveryPointer,
            final @Nullable String schemaFile,
            final CatalogSchemaSource source) {
        if (!comparesEveryPointer) {
            return docsSchemaMismatches(owner, type, requireNonNull(schemaFile, "schema"));
        }
        final List<String> pointers = jsonBodyPointers(type, source);
        assertThat(pointers)
                .as("JSON body pointers of the @CatalogSchema annotations")
                .isNotEmpty();
        return docsSchemaMismatches(type, pointers, source);
    }

    /**
     * Differences between a type and the schema of every pointer it covers.
     *
     * <p>One pointer: the differences of that schema. Several pointers say the same Java type stands for several nodes,
     * which may list different properties, so the rules are: a property or enum value that a pointer has and the type
     * lacks is a difference; one that the type has and <em>no</em> pointer has is a difference; any other difference of
     * a pointer, such as a type mismatch, counts. A property that only some pointers lack is fine.
     *
     * @param type the type under test
     * @param pointers the {@code @CatalogSchema} pointers the source covers
     * @param source gives the schema of a pointer
     * @return the differences, each prefixed with its pointer when there are several; empty when the type matches
     */
    public static List<String> docsSchemaMismatches(
            final Class<?> type, final List<String> pointers, final CatalogSchemaSource source) {
        final Map<String, List<String>> byPointer = new LinkedHashMap<>();
        for (final String pointer : pointers) {
            final JsonNode schema = requireNonNull(TREE.readTree(source.schemaJson(pointer)), "schema");
            byPointer.put(pointer, JavaDocsSchemaComparator.diff(type, schema));
        }
        if (byPointer.size() == 1) {
            return byPointer.values().iterator().next();
        }
        final List<String> found = new ArrayList<>();
        byPointer.forEach((pointer, differences) -> differences.stream()
                .filter(difference -> !isExtraOnJava(difference))
                .forEach(difference -> found.add(pointer + " " + difference)));
        Set<String> common = null;
        for (final List<String> differences : byPointer.values()) {
            final Set<String> extra = differences.stream()
                    .filter(CatalogChecks::isExtraOnJava)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (common == null) {
                common = extra;
            } else {
                common.retainAll(extra);
            }
        }
        if (common != null) {
            common.forEach(difference -> found.add("no pointer has it: " + difference));
        }
        return found;
    }

    /**
     * The record components of a type whose {@code @SuppressSchemaTypeMismatch} hides nothing: with the annotation
     * ignored, the type still matches the schema of every pointer it covers. A suppression that one pointer needs
     * stays.
     *
     * @param type the type under test
     * @param pointers the {@code @CatalogSchema} pointers the source covers
     * @param source gives the schema of a pointer
     * @return the JSON paths whose suppression can be removed, in the order of the first pointer
     */
    public static List<String> unnecessarySuppressions(
            final Class<?> type, final List<String> pointers, final CatalogSchemaSource source) {
        List<String> unnecessary = null;
        for (final String pointer : pointers) {
            final JsonNode schema = requireNonNull(TREE.readTree(source.schemaJson(pointer)), "schema");
            final List<String> found = JavaDocsSchemaComparator.unnecessarySuppressions(type, schema);
            if (unnecessary == null) {
                unnecessary = new ArrayList<>(found);
            } else {
                unnecessary.retainAll(found);
            }
        }
        return unnecessary == null ? List.of() : List.copyOf(unnecessary);
    }

    /**
     * The record components, across many types, whose {@code @SuppressSchemaTypeMismatch} hides nothing. Types without
     * a suppression are skipped.
     *
     * @param types the types under test, usually every {@code @CatalogSchema} type of the module
     * @param source gives the schema of a pointer
     * @return {@code TypeName.jsonPath} of each component whose suppression can be removed
     */
    public static List<String> unnecessarySuppressions(
            final Collection<? extends Class<?>> types, final CatalogSchemaSource source) {
        final List<String> unnecessary = new ArrayList<>();
        for (final Class<?> type : types) {
            if (!hasSuppression(type)) {
                continue;
            }
            unnecessarySuppressions(type, jsonBodyPointers(type, source), source)
                    .forEach(path -> unnecessary.add(type.getName() + "." + path));
        }
        return List.copyOf(unnecessary);
    }

    private static boolean hasSuppression(final Class<?> type) {
        return type.isRecord()
                && Arrays.stream(type.getRecordComponents())
                        .anyMatch(component -> component.isAnnotationPresent(SuppressSchemaTypeMismatch.class)
                                || component.getAccessor().isAnnotationPresent(SuppressSchemaTypeMismatch.class));
    }

    private static boolean isExtraOnJava(final String difference) {
        // A property the type has but a pointer lacks, or that the type accepts as absent although a pointer requires
        // it,
        // is fine when another pointer needs exactly that.
        return difference.endsWith(", missing on docs schema")
                || difference.endsWith(", missing on docs")
                || difference.endsWith("docs required, java @Nullable");
    }
}
