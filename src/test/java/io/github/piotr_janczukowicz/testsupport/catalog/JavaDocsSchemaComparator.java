package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.OptionalNull;
import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import io.github.piotr_janczukowicz.openapi.model.CatalogSimpleXor;
import io.github.piotr_janczukowicz.openapi.model.SuppressSchemaTypeMismatch;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Diffs a Java type (Jackson snake_case JSON names, or {@code @JsonProperty}) against a GitHub docs JSON Schema object.
 *
 * <p>Does not load the OpenAPI dump. Call once per Java type with the schema copied from the docs page. {@code format:
 * uri} must be {@link String} or {@link URI}. {@code format: int64} must be {@code long} or {@link Long}.
 * {@code format: date-time} must be {@link Instant}. {@code format: date} must be {@link LocalDate} or {@link String}.
 * {@code type} {@code number} must be {@code double} or {@link Double} (or {@code float}/{@link Float}, or
 * {@link BigDecimal}). Docs {@code required} must match {@link Nullable}: omitted {@code required} means every property
 * is {@code @Nullable} (a primitive cannot be, so an optional property must not be a primitive: a missing value would
 * read as {@code 0} or {@code false}); listed names must not be {@code @Nullable} unless that property's {@code type}
 * includes {@code null} (or {@code anyOf}/{@code oneOf} includes {@code null}); {@link OptionalNull} satisfies JSON
 * null without {@code @Nullable} on the wrapper; names not listed must be {@code @Nullable} and not a primitive. Docs
 * {@code enum} must match a Java enum's {@code wireValue()} set. An empty schema object ({@code items: {}}) is
 * unconstrained. Nested {@code allOf} without {@code properties} is merged into one object. Nested
 * {@code anyOf}/{@code oneOf} of {@code null} plus one object uses that object branch. Nested
 * {@code oneOf}/{@code anyOf} of only string {@code enum} branches is one Java enum ({@code wireValue} union). Nested
 * simple {@code oneOf}/{@code anyOf} of JSON scalars (no array) is {@link String} with {@link CatalogSimpleXor}. Nested
 * string-or-string-array, or scalar-plus-string-array (or 3+ kinds), is {@code List<String>} with
 * {@link CatalogSimpleXor}. JSON {@code null} is field nullability, not a {@code |null} in that annotation. Map values
 * use the same collapse. Nested {@code oneOf}/{@code anyOf} of {@code string} plus one array uses that array branch
 * when Java is a collection. Nested {@code oneOf}/{@code anyOf} of one object plus string and/or string-array branches
 * uses that object when the overlapping Java property is present (not {@code @Nullable}), as in encode-object-branch
 * payloads. When that property is {@code @Nullable} (flattened XOR with an ignored array branch), properties are
 * unioned; {@code required} is kept only for names required in every branch. Scalar-or-object XOR wrappers (every
 * property {@code @Nullable}) still skip the object branch when JSON names do not overlap; any other type is compared.
 * {@code minItems} ≥ 1 allows a non-{@code @Nullable} list when docs omit {@code required}. Unformatted {@code type:
 * string} may be {@link String} or {@link URI}; {@link Instant} and {@link LocalDate} differ from it and need
 * {@code @SuppressSchemaTypeMismatch}. Nested {@code oneOf}/{@code anyOf} object unions skip property matching when
 * Java is an interface (sealed location types). Root {@code properties} is an object even without {@code type: object}
 * (including {@code anyOf} required-only branches). A flattened record unions properties nested in {@code oneOf},
 * {@code anyOf}, and {@code allOf}. Names listed only in composition {@code required} arrays are unconstrained
 * properties when omitted from {@code properties}. Nested records with {@code @CatalogSchema} get a structural check
 * only (property names and kind; enums, required and nullability are left to their own CatalogTest); a nested
 * non-record against an object schema is a mismatch. A record with {@code @JsonValue} and no schema {@code properties}
 * is a JSON scalar, compared through its {@code @JsonValue} type (skipped when that type is {@code Object} or a
 * collection of {@code Object}). {@code additionalProperties} is an unconstrained map when {@code type} is
 * {@code object} or absent, including an empty {@code properties} object. A scalar {@code type} keeps its scalar check
 * even when {@code additionalProperties} is set. {@link SuppressSchemaTypeMismatch} skips the type check for that
 * component.
 */
public final class JavaDocsSchemaComparator {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final ThreadLocal<List<String>> UNNECESSARY = new ThreadLocal<>();

    private JavaDocsSchemaComparator() {}

    /**
     * Compares {@code javaType} JSON property names and nested types to {@code schema}.
     *
     * @param javaType mapped Java class
     * @param schema docs JSON Schema object
     * @return mismatch descriptions, empty when aligned
     */
    public static List<String> diff(final Class<?> javaType, final JsonNode schema) {
        requireNonNull(javaType, "javaType");
        requireNonNull(schema, "schema");
        final List<String> mismatches = new ArrayList<>();
        align("", MAPPER.constructType(javaType), schema, mismatches, null);
        return List.copyOf(mismatches);
    }

    /**
     * The record components that carry {@code @SuppressSchemaTypeMismatch} although their type matches the schema, so
     * the annotation can go.
     *
     * @param javaType mapped Java class
     * @param schema docs JSON Schema object
     * @return the JSON paths of the components whose suppression is not needed; empty when each one hides a mismatch
     */
    public static List<String> unnecessarySuppressions(final Class<?> javaType, final JsonNode schema) {
        requireNonNull(javaType, "javaType");
        requireNonNull(schema, "schema");
        final List<String> unnecessary = new ArrayList<>();
        UNNECESSARY.set(unnecessary);
        try {
            align("", MAPPER.constructType(javaType), schema, new ArrayList<>(), null);
        } finally {
            UNNECESSARY.remove();
        }
        return List.copyOf(unnecessary);
    }

    private static void align(
            final String path,
            final JavaType javaType,
            final JsonNode schema,
            final List<String> mismatches,
            final @Nullable String simpleXor) {
        if (javaType.getRawClass() == OptionalNull.class) {
            final JavaType wrapped = javaType.containedType(0);
            if (wrapped != null) {
                align(path, wrapped, schema, mismatches, simpleXor);
                return;
            }
        }
        if (unconstrained(schema)) {
            return;
        }
        if (jsonValueScalar(javaType.getRawClass()) && !schema.has("properties")) {
            alignJsonValue(path, javaType, schema, mismatches, simpleXor);
            return;
        }
        if (!path.isEmpty() && catalogNestedType(javaType.getRawClass())) {
            alignStructure(path, javaType, schema, mismatches);
            return;
        }
        if ((schema.has("oneOf") || schema.has("anyOf")) && !schema.has("properties")) {
            final JsonNode objectBranch = exclusiveObjectBranch(schema);
            if (objectBranch != null) {
                if (javaLacksObjectProperties(javaType, objectBranch) && isXorWrapper(javaType, schema)) {
                    return;
                }
                if (javaObjectBranchPropertyPresent(javaType, objectBranch)) {
                    align(path, javaType, objectBranch, mismatches, simpleXor);
                    return;
                }
            }
            final JsonNode mergedEnums = mergedStringEnums(schema);
            if (mergedEnums != null) {
                alignEnum(path, javaType.getRawClass(), mergedEnums, mismatches);
                return;
            }
            final Optional<List<String>> xorKinds = simpleXorKinds(schema);
            if (xorKinds.isPresent()) {
                alignSimpleXor(path, javaType, xorKinds.get(), simpleXor, mismatches);
                return;
            }
            if (javaType.isCollectionLikeType()) {
                final JsonNode arrayBranch = exclusiveArrayBranch(schema);
                if (arrayBranch != null) {
                    alignArray(path, javaType, arrayBranch, mismatches);
                    return;
                }
            }
            if (javaType.getRawClass().isInterface() && !javaType.getRawClass().isSealed()) {
                return;
            }
            alignObject(path, javaType, compositionUnion(schema), mismatches);
            return;
        }
        if (schema.has("allOf") && !schema.has("properties")) {
            final JsonNode merged = mergeAllOf(schema);
            if (merged != null) {
                align(path, javaType, merged, mismatches, simpleXor);
                return;
            }
        }
        if (schema.has("additionalProperties") && !hasProperties(schema) && additionalPropertiesMeansMap(schema)) {
            alignMap(path, javaType, schema, mismatches, simpleXor);
            return;
        }
        if (schema.has("properties")) {
            alignObject(path, javaType, mergeCompositionIntoProperties(schema), mismatches);
            return;
        }
        if (hasType(schema, "array")) {
            alignArray(path, javaType, schema, mismatches);
            return;
        }
        alignScalar(path, javaType, schema, mismatches);
    }

    private static void alignJsonValue(
            final String path,
            final JavaType javaType,
            final JsonNode schema,
            final List<String> mismatches,
            final @Nullable String simpleXor) {
        for (final Method method : javaType.getRawClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.isAnnotationPresent(JsonValue.class)) {
                final JavaType valueType = MAPPER.constructType(method.getGenericReturnType());
                if (!isHeterogeneous(valueType)) {
                    align(path, valueType, schema, mismatches, simpleXor);
                }
                return;
            }
        }
    }

    private static boolean isHeterogeneous(final JavaType valueType) {
        return valueType.getRawClass() == Object.class
                || valueType.isCollectionLikeType()
                        && valueType.getContentType().getRawClass() == Object.class;
    }

    private static void alignStructure(
            final String path, final JavaType javaType, final JsonNode schema, final List<String> mismatches) {
        final JsonNode object = structuralObject(javaType, schema);
        if (object == null) {
            return;
        }
        final Class<?> raw = javaType.getRawClass();
        if (!raw.isRecord()) {
            mismatches.add(path + ": docs object, java " + javaType);
            return;
        }
        alignPropertyNames(path, propertyNames(object.get("properties")), properties(javaType), mismatches);
    }

    private static @Nullable JsonNode structuralObject(final JavaType javaType, final JsonNode schema) {
        JsonNode node = schema;
        if (node.has("allOf") && !node.has("properties")) {
            final JsonNode merged = mergeAllOf(node);
            if (merged == null) {
                return null;
            }
            node = merged;
        }
        if ((node.has("oneOf") || node.has("anyOf")) && !node.has("properties")) {
            final JsonNode objectBranch = exclusiveObjectBranch(node);
            if (objectBranch == null
                    || !hasProperties(objectBranch)
                    || javaLacksObjectProperties(javaType, objectBranch) && isXorWrapper(javaType, node)) {
                return null;
            }
            node = objectBranch;
        }
        if (!hasProperties(node)) {
            return null;
        }
        final JsonNode object = mergeCompositionIntoProperties(node);
        return javaLacksObjectProperties(javaType, object) && isAllNullable(javaType) ? null : object;
    }

    private static boolean isAllNullable(final JavaType javaType) {
        final List<JsonField> fields = properties(javaType);
        return !fields.isEmpty() && fields.stream().allMatch(JsonField::nullable);
    }

    private static boolean isXorWrapper(final JavaType javaType, final JsonNode schema) {
        int nonNullBranches = 0;
        for (final JsonNode branch : compositionBranches(schema)) {
            if (!hasType(branch, "null") || branch.has("properties")) {
                nonNullBranches++;
            }
        }
        return nonNullBranches >= 2 && isAllNullable(javaType);
    }

    private static boolean hasProperties(final JsonNode schema) {
        final JsonNode properties = schema.get("properties");
        return properties != null && properties.isObject() && !properties.isEmpty();
    }

    private static boolean additionalPropertiesMeansMap(final JsonNode schema) {
        final JsonNode declared = schema.get("type");
        if (declared == null || declared.isNull()) {
            return true;
        }
        return hasType(schema, "object");
    }

    private static boolean javaLacksObjectProperties(final JavaType javaType, final JsonNode objectBranch) {
        return sharedObjectBranchFields(javaType, objectBranch).isEmpty();
    }

    private static boolean javaObjectBranchPropertyPresent(final JavaType javaType, final JsonNode objectBranch) {
        for (final JsonField field : sharedObjectBranchFields(javaType, objectBranch)) {
            if (!field.nullable()) {
                return true;
            }
        }
        return false;
    }

    private static List<JsonField> sharedObjectBranchFields(final JavaType javaType, final JsonNode objectBranch) {
        final JsonNode properties = objectBranch.get("properties");
        if (properties == null || !properties.isObject() || properties.isEmpty()) {
            return List.of();
        }
        final Set<String> schemaNames = propertyNames(properties);
        final List<JsonField> shared = new ArrayList<>();
        for (final JsonField field : properties(javaType)) {
            if (schemaNames.contains(field.name())) {
                shared.add(field);
            }
        }
        return shared;
    }

    private static boolean minItemsAllowsAbsent(final JsonNode propertySchema) {
        final JsonNode minItems = propertySchema.get("minItems");
        return minItems == null || !minItems.isNumber() || minItems.intValue() < 1;
    }

    private static boolean minPropertiesAllowsAbsent(final JsonNode schema) {
        final JsonNode minProperties = schema.get("minProperties");
        final JsonNode properties = schema.get("properties");
        return minProperties == null
                || !minProperties.isNumber()
                || minProperties.intValue() < 1
                || properties == null
                || !properties.isObject()
                || properties.size() != 1;
    }

    @Nullable private static JsonNode exclusiveObjectBranch(final JsonNode schema) {
        JsonNode objectBranch = null;
        for (final JsonNode branch : compositionBranches(schema)) {
            if (hasType(branch, "null") && !branch.has("properties")) {
                continue;
            }
            if (simpleXorKind(branch) != null) {
                continue;
            }
            if (objectBranch != null) {
                return null;
            }
            objectBranch = branch;
        }
        return objectBranch;
    }

    @Nullable private static JsonNode exclusiveArrayBranch(final JsonNode schema) {
        JsonNode arrayBranch = null;
        for (final JsonNode branch : compositionBranches(schema)) {
            if (!hasType(branch, "array")) {
                continue;
            }
            if (arrayBranch != null) {
                return null;
            }
            arrayBranch = branch;
        }
        return arrayBranch;
    }

    private static @Nullable JsonNode mergedStringEnums(final JsonNode schema) {
        final ArrayNode enums = MAPPER.createArrayNode();
        final Set<String> seen = new LinkedHashSet<>();
        int stringEnumBranches = 0;
        for (final JsonNode branch : compositionBranches(schema)) {
            if (hasType(branch, "null") && !branch.has("enum")) {
                continue;
            }
            if (!hasType(branch, "string")
                    || !branch.has("enum")
                    || !branch.get("enum").isArray()) {
                return null;
            }
            stringEnumBranches++;
            addEnumValues(branch.get("enum"), enums, seen);
        }
        if (stringEnumBranches < 2) {
            return null;
        }
        return enums;
    }

    private static Optional<List<String>> simpleXorKinds(final JsonNode schema) {
        final List<String> kinds = new ArrayList<>();
        for (final JsonNode branch : compositionBranches(schema)) {
            if (hasType(branch, "null") && !hasType(branch, "array") && !hasType(branch, "string")) {
                continue;
            }
            final String kind = simpleXorKind(branch);
            if (kind == null) {
                return Optional.empty();
            }
            kinds.add(kind);
        }
        if (kinds.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(List.copyOf(kinds));
    }

    private static @Nullable String simpleXorKind(final JsonNode branch) {
        if (branch.has("properties")) {
            return null;
        }
        if (hasType(branch, "array")) {
            final JsonNode items = branch.get("items");
            if (items == null || items.has("properties") || !hasType(items, "string")) {
                return null;
            }
            return "array";
        }
        if (hasType(branch, "string")) {
            return "string";
        }
        if (hasType(branch, "integer")) {
            return "integer";
        }
        if (hasType(branch, "number")) {
            return "number";
        }
        if (hasType(branch, "boolean")) {
            return "boolean";
        }
        return null;
    }

    private static void alignSimpleXor(
            final String path,
            final JavaType javaType,
            final List<String> kinds,
            final @Nullable String simpleXor,
            final List<String> mismatches) {
        final String expected = String.join("|", kinds);
        final boolean hasArray = kinds.contains("array");
        if (hasArray) {
            if (!javaType.isCollectionLikeType() || javaType.getContentType().getRawClass() != String.class) {
                mismatches.add(path + ": docs " + expected + ", java " + javaType);
                return;
            }
        } else if (javaType.getRawClass() != String.class) {
            mismatches.add(path + ": docs " + expected + ", java " + javaType);
            return;
        }
        if (simpleXor != null && !simpleXor.equals(expected)) {
            mismatches.add(path + ": docs " + expected + ", @CatalogSimpleXor " + simpleXor);
        }
    }

    private static List<JsonNode> compositionBranches(final JsonNode schema) {
        final List<JsonNode> branches = new ArrayList<>();
        addBranches(schema.get("oneOf"), branches);
        addBranches(schema.get("anyOf"), branches);
        return branches;
    }

    private static void addBranches(final JsonNode node, final List<JsonNode> branches) {
        if (node == null || node.isMissingNode() || !node.isArray()) {
            return;
        }
        for (final JsonNode branch : node) {
            branches.add(branch);
        }
    }

    private static JsonNode mergeCompositionIntoProperties(final JsonNode schema) {
        if (!schema.has("oneOf") && !schema.has("anyOf") && !schema.has("allOf")) {
            return schema;
        }
        final JsonNode propertiesNode = schema.get("properties");
        if (propertiesNode == null || !propertiesNode.isObject()) {
            return schema;
        }
        final JsonNode copy = schema.deepCopy();
        final ObjectNode properties = (ObjectNode) copy.get("properties");
        collectBranchProperties(copy.get("oneOf"), properties);
        collectBranchProperties(copy.get("anyOf"), properties);
        collectBranchProperties(copy.get("allOf"), properties);
        return copy;
    }

    private static JsonNode compositionUnion(final JsonNode schema) {
        final ObjectNode union = MAPPER.createObjectNode();
        union.put("type", "object");
        final ObjectNode properties = union.putObject("properties");
        collectBranchProperties(schema.get("oneOf"), properties);
        collectBranchProperties(schema.get("anyOf"), properties);
        final Set<String> required = requiredInEveryBranch(compositionBranches(schema));
        if (!required.isEmpty()) {
            final ArrayNode requiredNode = union.putArray("required");
            required.forEach(requiredNode::add);
        }
        return union;
    }

    private static Set<String> requiredInEveryBranch(final List<JsonNode> branches) {
        Set<String> common = null;
        for (final JsonNode branch : branches) {
            final JsonNode requiredNode = branch.get("required");
            final Set<String> names =
                    requiredNode != null && requiredNode.isArray() ? requiredNames(requiredNode) : Set.of();
            if (common == null) {
                common = new LinkedHashSet<>(names);
            } else {
                common.retainAll(names);
            }
        }
        return common == null ? Set.of() : common;
    }

    @Nullable private static JsonNode mergeAllOf(final JsonNode schema) {
        final JsonNode branches = schema.get("allOf");
        if (branches == null || !branches.isArray()) {
            return null;
        }
        final ObjectNode merged = MAPPER.createObjectNode();
        merged.put("type", "object");
        final ObjectNode properties = merged.putObject("properties");
        final ArrayNode required = MAPPER.createArrayNode();
        collectBranchProperties(branches, properties);
        for (final JsonNode branch : branches) {
            final JsonNode branchRequired = branch.get("required");
            if (branchRequired == null || !branchRequired.isArray()) {
                continue;
            }
            for (final JsonNode name : branchRequired) {
                required.add(name);
            }
        }
        if (!required.isEmpty()) {
            merged.set("required", required);
        }
        if (properties.isEmpty()) {
            return null;
        }
        return merged;
    }

    private static void collectBranchProperties(final JsonNode branches, final ObjectNode properties) {
        if (branches == null || branches.isMissingNode() || !branches.isArray()) {
            return;
        }
        for (final JsonNode branch : branches) {
            collectNodeProperties(branch, properties);
        }
    }

    private static void collectNodeProperties(final JsonNode branch, final ObjectNode properties) {
        if (branch == null || branch.isMissingNode() || !branch.isObject()) {
            return;
        }
        final JsonNode branchProperties = branch.get("properties");
        if (branchProperties != null && branchProperties.isObject()) {
            branchProperties
                    .properties()
                    .forEach(entry -> mergeBranchProperty(properties, entry.getKey(), entry.getValue()));
        }
        final JsonNode required = branch.get("required");
        if (required != null && required.isArray()) {
            for (final JsonNode name : required) {
                if (name.isString() && !properties.has(name.asString())) {
                    properties.putObject(name.asString());
                }
            }
        }
        collectBranchProperties(branch.get("oneOf"), properties);
        collectBranchProperties(branch.get("anyOf"), properties);
        collectBranchProperties(branch.get("allOf"), properties);
    }

    private static void mergeBranchProperty(final ObjectNode properties, final String name, final JsonNode incoming) {
        final JsonNode existing = properties.get(name);
        if (existing == null || !existing.isObject() || !incoming.isObject()) {
            properties.set(name, incoming);
            return;
        }
        final JsonNode unionedObjects = unionObjectShapes(existing, incoming);
        if (unionedObjects != null) {
            properties.set(name, unionedObjects);
            return;
        }
        final JsonNode existingEnum = existing.get("enum");
        final JsonNode incomingEnum = incoming.get("enum");
        if (existingEnum == null || !existingEnum.isArray() || incomingEnum == null || !incomingEnum.isArray()) {
            properties.set(name, incoming);
            return;
        }
        final ObjectNode merged = (ObjectNode) incoming.deepCopy();
        final ArrayNode enums = MAPPER.createArrayNode();
        final Set<String> seen = new LinkedHashSet<>();
        addEnumValues(existingEnum, enums, seen);
        addEnumValues(incomingEnum, enums, seen);
        merged.set("enum", enums);
        properties.set(name, merged);
    }

    private static @Nullable JsonNode unionObjectShapes(final JsonNode existing, final JsonNode incoming) {
        if (existing.equals(incoming)) {
            return null;
        }
        if (hasProperties(existing) && hasProperties(incoming)) {
            final ObjectNode merged = (ObjectNode) existing.deepCopy();
            final ObjectNode properties = (ObjectNode) merged.get("properties");
            incoming.get("properties")
                    .properties()
                    .forEach(entry -> mergeBranchProperty(properties, entry.getKey(), entry.getValue()));
            merged.remove("required");
            return merged;
        }
        if (hasType(existing, "array")
                && hasType(incoming, "array")
                && existing.has("items")
                && incoming.has("items")) {
            final JsonNode items = unionObjectShapes(existing.get("items"), incoming.get("items"));
            if (items == null) {
                return null;
            }
            final ObjectNode merged = (ObjectNode) existing.deepCopy();
            merged.set("items", items);
            return merged;
        }
        return null;
    }

    private static void addEnumValues(final JsonNode enumNode, final ArrayNode enums, final Set<String> seen) {
        for (final JsonNode item : enumNode) {
            final String key = item.isNull() ? "null" : item.isString() ? "s:" + item.asString() : item.toString();
            if (!seen.add(key)) {
                continue;
            }
            enums.add(item.deepCopy());
        }
    }

    private static void alignMap(
            final String path,
            final JavaType javaType,
            final JsonNode schema,
            final List<String> mismatches,
            final @Nullable String simpleXor) {
        if (!javaType.isMapLikeType()) {
            mismatches.add(path + ": docs additionalProperties object, java " + javaType);
            return;
        }
        final JsonNode additional = schema.get("additionalProperties");
        if (additional == null || additional.isBoolean()) {
            return;
        }
        align(path, javaType.getContentType(), additional, mismatches, simpleXor);
    }

    private static void alignObject(
            final String path, final JavaType javaType, final JsonNode schema, final List<String> mismatches) {
        if (javaType.isMapLikeType() || javaType.isCollectionLikeType() || javaType.isPrimitive()) {
            mismatches.add(path + ": docs object, java " + javaType);
            return;
        }
        final Set<String> schemaNames = propertyNames(schema.get("properties"));
        if (isSealedInterface(javaType.getRawClass())) {
            alignPropertyNames(path, schemaNames, permitProperties(javaType.getRawClass()), mismatches);
            return;
        }
        final List<JsonField> javaProperties = properties(javaType);
        alignPropertyNames(path, schemaNames, javaProperties, mismatches);
        for (final JsonField property : javaProperties) {
            final String name = property.name();
            if (!schemaNames.contains(name)) {
                continue;
            }
            alignNullable(
                    join(path, name),
                    property.nullable(),
                    property.type(),
                    schema,
                    name,
                    schema.get("properties").get(name),
                    mismatches);
            if (property.suppressTypeMismatch()) {
                recordIfUnnecessary(
                        join(path, name), property, schema.get("properties").get(name));
                continue;
            }
            align(
                    join(path, name),
                    property.type(),
                    schema.get("properties").get(name),
                    mismatches,
                    property.simpleXor());
        }
    }

    // Only set while unnecessarySuppressions runs: the type check is then done as if the annotation were absent.
    private static void recordIfUnnecessary(final String path, final JsonField property, final JsonNode schema) {
        final List<String> unnecessary = UNNECESSARY.get();
        if (unnecessary == null) {
            return;
        }
        final List<String> withoutSuppression = new ArrayList<>();
        align(path, property.type(), schema, withoutSuppression, property.simpleXor());
        if (withoutSuppression.isEmpty()) {
            unnecessary.add(path);
        }
    }

    private static boolean isSealedInterface(final Class<?> raw) {
        return raw.isInterface() && raw.isSealed();
    }

    private static List<JsonField> permitProperties(final Class<?> sealedInterface) {
        final List<JsonField> union = new ArrayList<>();
        for (final Class<?> permit : sealedInterface.getPermittedSubclasses()) {
            if (jsonValueScalar(permit)) {
                continue;
            }
            union.addAll(properties(MAPPER.constructType(permit)));
        }
        return union;
    }

    private static void alignPropertyNames(
            final String path,
            final Set<String> schemaNames,
            final List<JsonField> javaProperties,
            final List<String> mismatches) {
        final Set<String> javaNames = new LinkedHashSet<>();
        for (final JsonField property : javaProperties) {
            javaNames.add(property.name());
        }
        for (final String name : schemaNames) {
            if (!javaNames.contains(name)) {
                mismatches.add(join(path, name) + ": on docs schema, missing on java");
            }
        }
        for (final String name : javaNames) {
            if (!schemaNames.contains(name)) {
                mismatches.add(join(path, name) + ": on java, missing on docs schema");
            }
        }
    }

    private static void alignArray(
            final String path, final JavaType javaType, final JsonNode schema, final List<String> mismatches) {
        if (!javaType.isCollectionLikeType()) {
            mismatches.add(path + ": docs array, java " + javaType);
            return;
        }
        final JsonNode items = schema.get("items");
        if (items == null || items.isMissingNode()) {
            mismatches.add(path + ": docs array without items");
            return;
        }
        align(path, javaType.getContentType(), items, mismatches, null);
    }

    private static void alignNullable(
            final String path,
            final boolean nullable,
            final JavaType javaType,
            final JsonNode schema,
            final String propertyName,
            final JsonNode propertySchema,
            final List<String> mismatches) {
        final boolean primitive = javaType.isPrimitive();
        final JsonNode requiredNode = schema.get("required");
        if (requiredNode == null || requiredNode.isNull()) {
            if (!nullable && !primitive && minItemsAllowsAbsent(propertySchema) && minPropertiesAllowsAbsent(schema)) {
                mismatches.add(path + ": docs omitted required, java missing @Nullable");
            }
            if (primitive && minItemsAllowsAbsent(propertySchema) && minPropertiesAllowsAbsent(schema)) {
                mismatches.add(path + ": docs omitted required, java primitive cannot be absent");
            }
            return;
        }
        if (!requiredNode.isArray()) {
            mismatches.add(path + ": docs required is not an array");
            return;
        }
        if (requiredNames(requiredNode).contains(propertyName)) {
            if (allowsNull(propertySchema)) {
                if (primitive) {
                    mismatches.add(path + ": docs type includes null, java primitive");
                    return;
                }
                if (!nullable && javaType.getRawClass() != OptionalNull.class) {
                    mismatches.add(path + ": docs type includes null, java missing @Nullable");
                }
                return;
            }
            if (nullable) {
                mismatches.add(path + ": docs required, java @Nullable");
            }
            return;
        }
        if (!nullable && !primitive && minPropertiesAllowsAbsent(schema)) {
            mismatches.add(path + ": docs not required, java missing @Nullable");
        }
        if (primitive && minPropertiesAllowsAbsent(schema)) {
            mismatches.add(path + ": docs not required, java primitive cannot be absent");
        }
    }

    private static Set<String> requiredNames(final JsonNode required) {
        final Set<String> names = new LinkedHashSet<>();
        for (final JsonNode item : required) {
            if (item.isString()) {
                names.add(item.asString());
            }
        }
        return names;
    }

    private static void alignScalar(
            final String path, final JavaType javaType, final JsonNode schema, final List<String> mismatches) {
        final Class<?> raw = javaType.getRawClass();
        if (hasType(schema, "integer")) {
            if (schema.has("enum") && schema.get("enum").isArray() && raw.isEnum()) {
                alignIntegerEnum(path, raw, schema.get("enum"), mismatches);
                return;
            }
            if (isFormat(schema, "int64")) {
                if (raw == long.class || raw == Long.class) {
                    return;
                }
                mismatches.add(path + ": docs format int64, java " + javaType);
                return;
            }
            if (isInteger(raw)) {
                return;
            }
        }
        if (hasType(schema, "string")) {
            if (isFormat(schema, "date-time")) {
                if (raw == Instant.class) {
                    return;
                }
                mismatches.add(path + ": docs format date-time, java " + javaType);
                return;
            }
            if (isFormat(schema, "uri")) {
                if (raw == String.class || raw == URI.class) {
                    return;
                }
                mismatches.add(path + ": docs format uri, java " + javaType);
                return;
            }
            if (isFormat(schema, "date")) {
                if (raw == LocalDate.class || raw == String.class) {
                    return;
                }
                mismatches.add(path + ": docs format date, java " + javaType);
                return;
            }
            if (schema.has("enum") && schema.get("enum").isArray()) {
                alignEnum(path, raw, schema.get("enum"), mismatches);
                return;
            }
            if (raw == Instant.class || raw == LocalDate.class) {
                mismatches.add(path + ": docs plain string, java " + javaType);
                return;
            }
            if (raw == String.class || raw.isEnum() || raw == URI.class) {
                return;
            }
        }
        if (hasType(schema, "number")
                && (raw == double.class
                        || raw == Double.class
                        || raw == float.class
                        || raw == Float.class
                        || raw == BigDecimal.class)) {
            return;
        }
        if (hasType(schema, "boolean") && (raw == boolean.class || raw == Boolean.class)) {
            return;
        }
        mismatches.add(path + ": docs " + schema.get("type") + ", java " + javaType);
    }

    private static void alignEnum(
            final String path, final Class<?> raw, final JsonNode enumNode, final List<String> mismatches) {
        if (!raw.isEnum()) {
            mismatches.add(path + ": docs enum, java " + raw.getTypeName());
            return;
        }
        final Method wireValue = wireValueMethod(raw);
        if (wireValue == null) {
            mismatches.add(path + ": java enum missing wireValue()");
            return;
        }
        final Set<String> docsValues = stringEnumValues(enumNode);
        final Set<String> javaValues = javaWireValues(raw, wireValue);
        for (final String value : docsValues) {
            if (!javaValues.contains(value)) {
                mismatches.add(path + ": docs enum " + value + ", missing on java");
            }
        }
        reportExtraJavaValues(path, javaValues, docsValues, enumContainsNull(enumNode), mismatches);
    }

    private static Set<String> stringEnumValues(final JsonNode enumNode) {
        final Set<String> values = new LinkedHashSet<>();
        for (final JsonNode item : enumNode) {
            if (item.isString()) {
                values.add(item.asString());
            }
        }
        return values;
    }

    // A docs enum that lists null lets the Java enum add one constant that stands for it; any further constant is
    // drift.
    private static <V> void reportExtraJavaValues(
            final String path,
            final Set<V> javaValues,
            final Set<V> docsValues,
            final boolean isNullListed,
            final List<String> mismatches) {
        final List<V> extras = new ArrayList<>();
        for (final V value : javaValues) {
            if (!docsValues.contains(value)) {
                extras.add(value);
            }
        }
        final int allowed = isNullListed ? 1 : 0;
        if (extras.size() > allowed) {
            for (final V value : extras) {
                mismatches.add(path + ": java enum " + value + ", missing on docs");
            }
        }
    }

    private static boolean enumContainsNull(final JsonNode enumNode) {
        for (final JsonNode item : enumNode) {
            if (item.isNull()) {
                return true;
            }
        }
        return false;
    }

    private static Method wireValueMethod(final Class<?> raw) {
        try {
            final Method method = raw.getMethod("wireValue");
            if (method.getParameterCount() == 0 && method.getReturnType() == String.class) {
                return method;
            }
        } catch (final NoSuchMethodException ignored) {
            return null;
        }
        return null;
    }

    private static Set<String> javaWireValues(final Class<?> raw, final Method wireValue) {
        final Set<String> values = new LinkedHashSet<>();
        for (final Object constant : raw.getEnumConstants()) {
            try {
                values.add((String) wireValue.invoke(constant));
            } catch (final IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException(raw.getName() + " wireValue()", ex);
            }
        }
        return values;
    }

    private static void alignIntegerEnum(
            final String path, final Class<?> raw, final JsonNode enumNode, final List<String> mismatches) {
        final Method wireValue = integerWireValueMethod(raw);
        if (wireValue == null) {
            mismatches.add(path + ": java enum missing wireValue()");
            return;
        }
        if (!isInteger(wireValue.getReturnType())) {
            mismatches.add(path + ": java enum wireValue() not integer");
            return;
        }
        final Set<Long> docsValues = integerEnumValues(enumNode);
        final Set<Long> javaValues = javaIntegerWireValues(raw, wireValue);
        for (final Long value : docsValues) {
            if (!javaValues.contains(value)) {
                mismatches.add(path + ": docs enum " + value + ", missing on java");
            }
        }
        reportExtraJavaValues(path, javaValues, docsValues, enumContainsNull(enumNode), mismatches);
    }

    private static Set<Long> integerEnumValues(final JsonNode enumNode) {
        final Set<Long> values = new LinkedHashSet<>();
        for (final JsonNode item : enumNode) {
            if (item.isIntegralNumber()) {
                values.add(item.asLong());
            }
        }
        return values;
    }

    private static Method integerWireValueMethod(final Class<?> raw) {
        try {
            return raw.getMethod("wireValue");
        } catch (final NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Set<Long> javaIntegerWireValues(final Class<?> raw, final Method wireValue) {
        final Set<Long> values = new LinkedHashSet<>();
        for (final Object constant : raw.getEnumConstants()) {
            try {
                values.add(((Number) wireValue.invoke(constant)).longValue());
            } catch (final IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException(raw.getName() + " wireValue()", ex);
            }
        }
        return values;
    }

    private static List<JsonField> properties(final JavaType javaType) {
        final Class<?> raw = javaType.getRawClass();
        if (!raw.isRecord()) {
            return List.of();
        }
        final List<JsonField> fields = new ArrayList<>();
        for (final RecordComponent component : raw.getRecordComponents()) {
            if (jsonIgnore(component)) {
                continue;
            }
            if (isUnwrapped(component)) {
                fields.addAll(properties(MAPPER.constructType(component.getGenericType())));
                continue;
            }
            fields.add(new JsonField(
                    jsonName(component),
                    MAPPER.constructType(component.getGenericType()),
                    hasNullable(component),
                    suppressTypeMismatch(component),
                    catalogSimpleXor(component)));
        }
        return List.copyOf(fields);
    }

    private static boolean isUnwrapped(final RecordComponent component) {
        final JsonUnwrapped unwrapped = component.getAnnotation(JsonUnwrapped.class) != null
                ? component.getAnnotation(JsonUnwrapped.class)
                : component.getAccessor().getAnnotation(JsonUnwrapped.class);
        if (unwrapped == null || !unwrapped.enabled()) {
            return false;
        }
        if (!unwrapped.prefix().isEmpty() || !unwrapped.suffix().isEmpty()) {
            throw new IllegalStateException(
                    component.getName() + ": @JsonUnwrapped prefix and suffix are not supported");
        }
        return true;
    }

    private static String jsonName(final RecordComponent component) {
        final JsonProperty property = jsonProperty(component);
        if (property != null) {
            final String explicit = property.value();
            if (!explicit.isEmpty() && !JsonProperty.USE_DEFAULT_NAME.equals(explicit)) {
                return explicit;
            }
        }
        return snakeCase(component.getName());
    }

    private static boolean catalogNestedType(final Class<?> raw) {
        if (!raw.isRecord() && !raw.isInterface() && !raw.isEnum()) {
            return false;
        }
        return raw.isAnnotationPresent(CatalogSchema.class);
    }

    private static boolean jsonValueScalar(final Class<?> raw) {
        if (!raw.isRecord()) {
            return false;
        }
        for (final Method method : raw.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.isAnnotationPresent(JsonValue.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean jsonIgnore(final RecordComponent component) {
        return component.isAnnotationPresent(JsonIgnore.class)
                || component.getAccessor().isAnnotationPresent(JsonIgnore.class);
    }

    private static JsonProperty jsonProperty(final RecordComponent component) {
        final JsonProperty onComponent = component.getAnnotation(JsonProperty.class);
        if (onComponent != null) {
            return onComponent;
        }
        return component.getAccessor().getAnnotation(JsonProperty.class);
    }

    // The name the mapper writes: the client's JSON mappers use Jackson's SNAKE_CASE, which keeps a run of capitals
    // together (userID is user_id), so ask the strategy itself instead of repeating its rules.
    private static String snakeCase(final String javaName) {
        return PropertyNamingStrategies.SNAKE_CASE.nameForField(null, null, javaName);
    }

    private static @Nullable String catalogSimpleXor(final RecordComponent component) {
        final CatalogSimpleXor onComponent = component.getAnnotation(CatalogSimpleXor.class);
        if (onComponent != null) {
            return onComponent.value();
        }
        final CatalogSimpleXor onAccessor = component.getAccessor().getAnnotation(CatalogSimpleXor.class);
        if (onAccessor != null) {
            return onAccessor.value();
        }
        return null;
    }

    private static boolean suppressTypeMismatch(final RecordComponent component) {
        return component.isAnnotationPresent(SuppressSchemaTypeMismatch.class)
                || component.getAccessor().isAnnotationPresent(SuppressSchemaTypeMismatch.class);
    }

    private static boolean hasNullable(final RecordComponent component) {
        return component.isAnnotationPresent(Nullable.class)
                || component.getAnnotatedType().isAnnotationPresent(Nullable.class);
    }

    private static Set<String> propertyNames(final JsonNode properties) {
        final Set<String> names = new LinkedHashSet<>();
        properties.properties().forEach(entry -> names.add(entry.getKey()));
        return names;
    }

    private static boolean unconstrained(final JsonNode schema) {
        return schema.isObject()
                && !schema.has("type")
                && !schema.has("properties")
                && !schema.has("items")
                && !schema.has("additionalProperties")
                && !schema.has("oneOf")
                && !schema.has("anyOf")
                && !schema.has("allOf")
                && !schema.has("enum")
                && !schema.has("format")
                && !schema.has("$ref");
    }

    private static boolean hasType(final JsonNode schema, final String type) {
        final JsonNode declared = schema.get("type");
        if (declared == null || declared.isNull()) {
            return false;
        }
        if (declared.isString()) {
            return type.equals(declared.asString());
        }
        if (declared.isArray()) {
            for (final JsonNode item : declared) {
                if (type.equals(item.asString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean allowsNull(final JsonNode schema) {
        if (hasType(schema, "null")) {
            return true;
        }
        return compositionAllowsNull(schema.get("oneOf")) || compositionAllowsNull(schema.get("anyOf"));
    }

    private static boolean compositionAllowsNull(final JsonNode branches) {
        if (branches == null || branches.isMissingNode() || !branches.isArray()) {
            return false;
        }
        for (final JsonNode branch : branches) {
            if (hasType(branch, "null")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFormat(final JsonNode schema, final String format) {
        final JsonNode declared = schema.get("format");
        return declared != null && declared.isString() && format.equals(declared.asString());
    }

    private static boolean isInteger(final Class<?> raw) {
        return raw == int.class || raw == Integer.class || raw == long.class || raw == Long.class;
    }

    private static String join(final String path, final String name) {
        return path.isEmpty() ? name : path + "." + name;
    }

    private record JsonField(
            String name,
            JavaType type,
            boolean nullable,
            boolean suppressTypeMismatch,
            @Nullable String simpleXor) {}
}
