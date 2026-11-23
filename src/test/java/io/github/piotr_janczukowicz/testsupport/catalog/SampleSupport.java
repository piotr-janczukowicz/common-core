package io.github.piotr_janczukowicz.testsupport.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonType;
import io.github.piotr_janczukowicz.json.ValueUnion;
import io.github.piotr_janczukowicz.json.jackson.ValueUnionModule;
import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

final class SampleSupport {

    private static final JsonMapper MAPPER =
            JsonMapper.builder().addModule(new ValueUnionModule()).build();

    private static final Map<String, String> SCHEMAS = Map.of(
            "components/schemas/sample",
            """
            {"type":"object","properties":{"name":{"type":"string"},"count":{"type":"integer"}},"required":["name","count"]}
            """,
            "components/schemas/sample-tuple",
            """
            {"type":"array","prefixItems":[{"type":"integer","format":"int64"},{"type":"string"}],"items":false,"minItems":2,"maxItems":2}
            """,
            "components/schemas/sample-text",
            """
            {"type":"object","properties":{"text":{"type":"string"}},"required":["text"]}
            """,
            "components/schemas/sample-number",
            """
            {"type":"object","properties":{"number":{"type":"integer"}},"required":["number"]}
            """,
            "components/schemas/sample-multi-a",
            """
            {"type":"object","properties":{"name":{"type":"string"},"extra":{"type":"string"}},"required":["name"]}
            """,
            "components/schemas/sample-multi-b",
            """
            {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
            """,
            "components/schemas/sample-holder",
            """
            {"type":"object","properties":{"kind":{"type":"string","enum":["empty","named","wrapped"]},
            "payload":{"oneOf":[
            {"type":"object","properties":{"note":{"type":"string"}}},
            {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]},
            {"type":"object","properties":{"url":{"type":"string"}},"required":["url"]}]}},
            "required":["kind","payload"]}
            """,
            "components/schemas/sample-branch-empty",
            """
            {"type":"object","properties":{"note":{"type":"string"}}}
            """,
            "components/schemas/sample-branch-named",
            """
            {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
            """,
            "components/schemas/sample-branch-wrapped",
            """
            {"type":"object","properties":{"url":{"type":"string"}},"required":["url"]}
            """);

    private SampleSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    static JsonCodec codec() {
        return new JsonCodec() {
            @Override
            public <T> T decode(final byte[] json, final Class<T> type) {
                return MAPPER.readValue(json, type);
            }

            @Override
            public <T> T decode(final byte[] json, final JsonType<T> type) {
                return MAPPER.readValue(json, MAPPER.constructType(type.javaType()));
            }

            @Override
            public byte[] encode(final Object value) {
                return MAPPER.writeValueAsBytes(value);
            }
        };
    }

    static CatalogSchemaSource schemaSource() {
        return new CatalogSchemaSource() {
            @Override
            public boolean covers(final String pointer) {
                return SCHEMAS.containsKey(pointer);
            }

            @Override
            public String schemaJson(final String pointer) {
                return SCHEMAS.get(pointer);
            }
        };
    }

    @CatalogSchema("components/schemas/sample")
    record Sample(String name, int count) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"fieldId", "direction"})
    @CatalogSchema("components/schemas/sample-tuple")
    record SampleTuple(long fieldId, String direction) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @CatalogSchema({"components/schemas/sample-multi-a", "components/schemas/sample-multi-b"})
    record SampleMulti(String name, @Nullable String extra) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @CatalogSchema({"components/schemas/sample-text", "components/schemas/sample-number"})
    record SampleXor(@Nullable String text, @Nullable Integer number) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @CatalogSchema("components/schemas/sample-holder")
    record SampleHolder(
            String kind,

            @JsonTypeInfo(
                    use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                    property = "kind",
                    defaultImpl = SampleBranchUnknown.class)
            SampleBranch payload) {}

    @JsonSubTypes({
        @JsonSubTypes.Type(value = SampleBranchEmpty.class, name = "empty"),
        @JsonSubTypes.Type(value = SampleBranchNamed.class, name = "named"),
        @JsonSubTypes.Type(value = SampleBranchWrapped.class, name = "wrapped")
    })
    sealed interface SampleBranch
            permits SampleBranchEmpty, SampleBranchNamed, SampleBranchWrapped, SampleBranchUnknown {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @CatalogSchema("components/schemas/sample-branch-empty")
    record SampleBranchEmpty(@Nullable String note) implements SampleBranch {}

    @CatalogSchema("components/schemas/sample-branch-named")
    record SampleBranchNamed(String name) implements SampleBranch {}

    record SampleBranchWrapped(@JsonValue SampleWrappedConfig config) implements SampleBranch {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        SampleBranchWrapped {}
    }

    @CatalogSchema("components/schemas/sample-branch-wrapped")
    record SampleWrappedConfig(String url) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    record SampleBranchUnknown(@JsonValue Map<String, Object> properties) implements SampleBranch {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(SampleCat.class), @JsonSubTypes.Type(SampleDog.class)})
    sealed interface SampleUnion permits SampleCat, SampleDog {}

    record SampleCat(String meow) implements SampleUnion {}

    record SampleDog(String bark) implements SampleUnion {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes(@JsonSubTypes.Type(SampleBird.class))
    sealed interface SampleLenient permits SampleBird, SampleFallback {}

    record SampleBird(String tweet) implements SampleLenient {}

    record SampleFallback(String note) implements SampleLenient {}

    /** A value union whose object branch is a union of its own: it permits a sealed interface. */
    @ValueUnion(string = SampleNestedText.class, object = SampleNestedObject.class)
    sealed interface SampleNested permits SampleNestedText, SampleNestedObject {}

    record SampleNestedText(@JsonValue String text) implements SampleNested {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        SampleNestedText {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({@JsonSubTypes.Type(SampleNestedBird.class), @JsonSubTypes.Type(SampleNestedCat.class)})
    sealed interface SampleNestedObject extends SampleNested permits SampleNestedBird, SampleNestedCat {}

    record SampleNestedBird(String tweet) implements SampleNestedObject {}

    record SampleNestedCat(String meow) implements SampleNestedObject {}

    enum SampleState {
        @JsonProperty("open")
        OPEN,
        @JsonProperty("closed")
        CLOSED
    }

    /** Docs value {@code unknown} is real, so the fallback of the library is {@code UNRECOGNIZED}. */
    enum SampleValidity {
        @JsonProperty("active")
        ACTIVE,
        @JsonProperty("unknown")
        UNKNOWN,
        @JsonProperty("unrecognized")
        UNRECOGNIZED
    }
}
