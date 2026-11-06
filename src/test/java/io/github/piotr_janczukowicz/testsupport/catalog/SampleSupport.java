package io.github.piotr_janczukowicz.testsupport.catalog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonType;
import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

final class SampleSupport {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final Map<String, String> SCHEMAS = Map.of(
            "components/schemas/sample",
            """
            {"type":"object","properties":{"name":{"type":"string"},"count":{"type":"integer"}},"required":["name","count"]}
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
    @CatalogSchema({"components/schemas/sample-multi-a", "components/schemas/sample-multi-b"})
    record SampleMulti(String name, @Nullable String extra) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @CatalogSchema({"components/schemas/sample-text", "components/schemas/sample-number"})
    record SampleXor(@Nullable String text, @Nullable Integer number) {}

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
