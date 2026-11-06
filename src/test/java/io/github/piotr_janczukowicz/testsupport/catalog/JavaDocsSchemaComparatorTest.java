package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import io.github.piotr_janczukowicz.openapi.model.CatalogSimpleXor;
import io.github.piotr_janczukowicz.openapi.model.SuppressSchemaTypeMismatch;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class JavaDocsSchemaComparatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void diff_shouldTreatAnyOfRequiredName_whenOmittedFromProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"https_enforced":{"type":"boolean"}},"anyOf":[{"required":["https_enforced"]},{"required":["public"]}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PagesUpdate.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldRequireNonNull_whenNameRequiredInEveryAnyOfBranch() {
        final JsonNode schema = MAPPER.readTree("""
                {"anyOf":[{"type":"object","properties":{"id":{"type":"integer"},"kind":{"type":"string"},"a":{"type":"string"}},"required":["id","kind"]},{"type":"object","properties":{"id":{"type":"integer"},"kind":{"type":"string"}},"required":["id","kind"]}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(AnyOfRequired.class, schema)).isEmpty();
        assertThat(JavaDocsSchemaComparator.diff(AnyOfNullableShared.class, schema))
                .contains("kind: docs required, java @Nullable");
    }

    @Test
    void diff_shouldRequireNullable_whenNameRequiredInOnlySomeAnyOfBranches() {
        final JsonNode schema = MAPPER.readTree("""
                {"anyOf":[{"type":"object","properties":{"id":{"type":"integer"},"a":{"type":"string"}},"required":["id","a"]},{"type":"object","properties":{"id":{"type":"integer"},"a":{"type":"string"}},"required":["id"]}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(AnyOfPartialRequired.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldSkipJsonIgnoreComponent_whenMissingOnDocsSchema() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"q":{"type":"string"}},"required":["q"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Query.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldSkipJsonValueRecordComponents_whenSchemaHasNoProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"anyOf":[{"type":"string"},{"type":"number"},{"type":"integer"}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Scalar.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldCompareNestedCatalogSchemaTypePropertyNames_whenDumpOmitsFields() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"license":{"type":"object","properties":{"key":{"type":"string"}}}},"required":["license"]}
                """);

        final List<String> mismatches = JavaDocsSchemaComparator.diff(Licensed.class, schema);

        assertThat(mismatches).contains("license.name: on java, missing on docs schema");
    }

    @Test
    void diff_shouldNotCompareNestedCatalogSchemaTypeEnumsOrRequired_whenPropertyNamesMatch() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"license":{"type":"object","properties":{"key":{"type":"string"},"name":{"type":"string"},"url":{"type":["string","null"]},"spdx_id":{"type":["string","null"]},"node_id":{"type":"string"},"html_url":{"type":"string"}}}},"required":["license"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Licensed.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldNameAcronymLikeTheMapper_whenComponentHasCapitalRun() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"user_id":{"type":"integer","format":"int64"}},"required":["user_id"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(CapitalRun.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportMismatch_whenJsonValueTypeDiffersFromSchema() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"string"}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Count.class, schema)).isNotEmpty();
    }

    @Test
    void diff_shouldAcceptJsonValueRecord_whenValueTypeMatchesSchema() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"integer"}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Count.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldCompareSealedInterfaceToPermitPropertyNames_whenSchemaHasProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"items":{"type":"array","items":{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"string"}}}}},"required":["items"]}
                """);

        final List<String> mismatches = JavaDocsSchemaComparator.diff(Holder.class, schema);

        assertThat(mismatches).containsExactly("items.b: on docs schema, missing on java");
    }

    @Test
    void diff_shouldUnionItemProperties_whenBranchesDefineSamePropertyDifferently() {
        final JsonNode schema = MAPPER.readTree("""
                {"anyOf":[{"type":"object","properties":{"items":{"type":"array","items":{"type":"object","properties":{"a":{"type":"string"}}}}}},{"type":"object","properties":{"items":{"type":"array","items":{"type":"object","properties":{"b":{"type":"string"}}}}}}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(FlatHolder.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportMismatch_whenObjectBranchSharesNoPropertiesAndJavaIsNotXorWrapper() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"type":"string"},{"type":"object","properties":{"name":{"type":"string"}}}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Unrelated.class, schema)).isNotEmpty();
    }

    @Test
    void diff_shouldSkipScalarType_whenSuppressSchemaTypeMismatch() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"verified_at":{"type":["string","null"]}},"required":["verified_at"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(SuppressedInstant.class, schema))
                .isEmpty();
    }

    @Test
    void unnecessarySuppressions_shouldListTheComponent_whenTheTypeMatchesWithoutIt() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"verified_at":{"type":["string","null"]}}}
                """);

        final List<String> unnecessary =
                JavaDocsSchemaComparator.unnecessarySuppressions(SuppressedString.class, schema);

        assertThat(unnecessary).containsExactly("verified_at");
    }

    @Test
    void unnecessarySuppressions_shouldBeEmpty_whenTheSuppressionHidesAMismatch() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"start_line":{"type":"number"}},"required":["start_line"]}
                """);

        final List<String> unnecessary = JavaDocsSchemaComparator.unnecessarySuppressions(SuppressedInt.class, schema);

        assertThat(unnecessary).isEmpty();
    }

    @Test
    void diff_shouldStillReportNullable_whenTypeSuppressed() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"verified_at":{"type":"string"}},"required":["verified_at"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(SuppressedInstant.class, schema))
                .containsExactly("verified_at: docs required, java @Nullable");
    }

    @Test
    void diff_shouldAcceptString_whenScalarXorAnnotated() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"id":{"oneOf":[{"type":"string"},{"type":"integer"}]}},"required":["id"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ScalarXor.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAcceptList_whenStringOrArrayXorAnnotated() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"labels":{"anyOf":[{"type":"string"},{"type":"array","items":{"type":"string"}}]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(StringOrArrayXor.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldAcceptList_whenMultiKindXorAnnotated() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"value":{"oneOf":[{"type":"string"},{"type":"number"},{"type":"array","items":{"type":"string"}}]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(MultiKindXor.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAcceptMapValues_whenScalarXorAnnotated() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"extra":{"type":"object","additionalProperties":{"oneOf":[{"type":"string"},{"type":"integer"}]}}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(MapXor.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportXorAnnotation_whenKindsDisagree() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"id":{"oneOf":[{"type":"string"},{"type":"number"}]}},"required":["id"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ScalarXor.class, schema))
                .containsExactly("id: docs string|number, @CatalogSimpleXor string|integer");
    }

    @Test
    void diff_shouldRequireObjectProperty_whenOneOfObjectPlusStringAndArray() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"type":"object","properties":{"emails":{"type":"array","items":{"type":"string"},"minItems":1}},"required":["emails"]},{"type":"array","items":{"type":"string"}},{"type":"string"}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(EmailsPayload.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldTreatObjectBranchNullable_whenOneOfObjectPlusArrayXor() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"type":"object","properties":{"contexts":{"type":"array","items":{"type":"string"}}},"required":["contexts"]},{"type":"array","items":{"type":"string"}}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(StatusCheckXorPayload.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldSkipObjectBranch_whenScalarOrObjectXorRecord() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"type":"string"},{"type":"object","properties":{"name":{"type":"string"}}}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(LabelXor.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAllowRequiredList_whenMinItemsAndOmittedRequired() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"type":"object","properties":{"labels":{"type":"array","items":{"type":"string"},"minItems":1}}},{"type":"array","items":{"type":"string"}},{"type":"string"}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(LabelsPayload.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAllowNonNullable_whenSingleOptionalPropertyWithMinProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"mode":{"type":"string"}},"minProperties":1}
                """);

        assertThat(JavaDocsSchemaComparator.diff(SinglePropertyMode.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldReportMissingNullable_whenSingleOptionalPropertyWithoutMinProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"mode":{"type":"string"}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(SinglePropertyMode.class, schema))
                .containsExactly("mode: docs omitted required, java missing @Nullable");
    }

    @Test
    void diff_shouldReportMissingNullable_whenMinPropertiesWithMultipleProperties() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"mode":{"type":"string"},"other":{"type":"string"}},"minProperties":1}
                """);

        assertThat(JavaDocsSchemaComparator.diff(TwoPropertyModes.class, schema))
                .containsExactlyInAnyOrder(
                        "mode: docs omitted required, java missing @Nullable",
                        "other: docs omitted required, java missing @Nullable");
    }

    @Test
    void diff_shouldReportPrimitive_whenPropertyIsOptionalAndRequiredIsOmitted() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"count":{"type":"integer"}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PrimitiveCount.class, schema))
                .containsExactly("count: docs omitted required, java primitive cannot be absent");
    }

    @Test
    void diff_shouldReportPrimitive_whenPropertyIsNotListedInRequired() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"count":{"type":"integer"},"name":{"type":"string"}},"required":["name"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PrimitiveCountAndName.class, schema))
                .containsExactly("count: docs not required, java primitive cannot be absent");
    }

    @Test
    void diff_shouldAcceptPrimitive_whenPropertyIsRequired() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"count":{"type":"integer"}},"required":["count"]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PrimitiveCount.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAcceptPrimitive_whenMinPropertiesImpliesThePropertyIsPresent() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"count":{"type":"integer"}},"minProperties":1}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PrimitiveCount.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportInstantAndAcceptUri_whenStringOmitsFormat() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"url":{"type":"string"},"created_at":{"type":"string"}}}
                """);

        final List<String> mismatches = JavaDocsSchemaComparator.diff(EventTimes.class, schema);

        assertThat(mismatches).hasSize(1).first().asString().startsWith("created_at: docs plain string, java");
    }

    @Test
    void diff_shouldTreatExclusiveObjectFieldsNullable_whenOneOfTwoObjects() {
        final JsonNode schema = MAPPER.readTree("""
                {"oneOf":[{"properties":{"subject_digests":{"type":"array","items":{"type":"string"}}},"required":["subject_digests"]},{"properties":{"attestation_ids":{"type":"array","items":{"type":"integer"}}},"required":["attestation_ids"]}]}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ExclusiveObjectPayload.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldAcceptLocalDate_whenFormatDate() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"start":{"type":"string","format":"date"}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(Dated.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldAcceptEnum_whenIntegerEnumMatches() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":"integer","enum":[0,15,30,45]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(MinuteField.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportMissingValue_whenIntegerEnumLacksDocsValue() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":"integer","enum":[0,15,30,45]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(MissingMinuteField.class, schema))
                .containsExactly("minute: docs enum 45, missing on java");
    }

    @Test
    void diff_shouldAcceptOneExtraValue_whenIntegerEnumListsNull() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":["integer","null"],"enum":[0,15,30,45,null]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ExtraMinuteField.class, schema))
                .isEmpty();
    }

    @Test
    void diff_shouldReportExtraValues_whenIntegerEnumListsNullButJavaHasTwoMore() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":["integer","null"],"enum":[0,15,30,null]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ExtraMinuteField.class, schema))
                .containsExactly("minute: java enum 45, missing on docs", "minute: java enum 60, missing on docs");
    }

    @Test
    void diff_shouldAcceptOneExtraValue_whenStringEnumListsNull() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"mode":{"type":["string","null"],"enum":["on","off",null]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ModeField.class, schema)).isEmpty();
    }

    @Test
    void diff_shouldReportExtraValues_whenStringEnumListsNullButJavaHasTwoMore() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"mode":{"type":["string","null"],"enum":["on",null]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ModeField.class, schema))
                .containsExactly("mode: java enum off, missing on docs", "mode: java enum none, missing on docs");
    }

    @Test
    void diff_shouldReportExtraValue_whenIntegerEnumHasJavaOnlyValue() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":"integer","enum":[0,15,30,45]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(ExtraMinuteField.class, schema))
                .containsExactly("minute: java enum 60, missing on docs");
    }

    @Test
    void diff_shouldReportNonInteger_whenIntegerEnumWireValueIsString() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":"integer","enum":[0,15,30,45]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(StringWireMinuteField.class, schema))
                .containsExactly("minute: java enum wireValue() not integer");
    }

    @Test
    void diff_shouldAcceptInteger_whenPlainIntegerAgainstIntegerEnumSchema() {
        final JsonNode schema = MAPPER.readTree("""
                {"type":"object","properties":{"minute":{"type":"integer","enum":[0,15,30,45]}}}
                """);

        assertThat(JavaDocsSchemaComparator.diff(PlainMinuteField.class, schema))
                .isEmpty();
    }

    record ScalarXor(@CatalogSimpleXor("string|integer") String id) {}

    record StringOrArrayXor(
            @Nullable @CatalogSimpleXor("string|array") List<String> labels) {}

    record MultiKindXor(
            @Nullable @CatalogSimpleXor("string|number|array")
            List<String> value) {}

    record MapXor(
            @Nullable @CatalogSimpleXor("string|integer") Map<String, String> extra) {}

    record EmailsPayload(List<String> emails) {}

    record StatusCheckXorPayload(
            @Nullable List<String> contexts,
            @JsonIgnore @Nullable List<String> contextList) {}

    record LabelsPayload(List<String> labels) {}

    record SinglePropertyMode(String mode) {}

    record PrimitiveCount(int count) {}

    record PrimitiveCountAndName(int count, String name) {}

    record TwoPropertyModes(String mode, String other) {}

    record LabelXor(@Nullable String labelName, @Nullable String labelDetails) {}

    record EventTimes(@Nullable URI url, @Nullable Instant createdAt) {}

    record ExclusiveObjectPayload(
            @Nullable List<String> subjectDigests, @Nullable List<Long> attestationIds) {}

    record Dated(@Nullable LocalDate start) {}

    enum Minute {
        ZERO(0),
        FIFTEEN(15),
        THIRTY(30),
        FORTY_FIVE(45);

        private final int value;

        Minute(final int value) {
            this.value = value;
        }

        public int wireValue() {
            return value;
        }
    }

    enum MissingMinute {
        ZERO(0),
        FIFTEEN(15),
        THIRTY(30);

        private final int value;

        MissingMinute(final int value) {
            this.value = value;
        }

        public int wireValue() {
            return value;
        }
    }

    enum ExtraMinute {
        ZERO(0),
        FIFTEEN(15),
        THIRTY(30),
        FORTY_FIVE(45),
        SIXTY(60);

        private final int value;

        ExtraMinute(final int value) {
            this.value = value;
        }

        public int wireValue() {
            return value;
        }
    }

    enum StringWireMinute {
        ZERO("0"),
        FIFTEEN("15"),
        THIRTY("30"),
        FORTY_FIVE("45");

        private final String value;

        StringWireMinute(final String value) {
            this.value = value;
        }

        public String wireValue() {
            return value;
        }
    }

    enum Mode {
        ON("on"),
        OFF("off"),
        NONE("none");

        private final String value;

        Mode(final String value) {
            this.value = value;
        }

        public String wireValue() {
            return value;
        }
    }

    record ModeField(@Nullable Mode mode) {}

    record MinuteField(@Nullable Minute minute) {}

    record MissingMinuteField(@Nullable MissingMinute minute) {}

    record ExtraMinuteField(@Nullable ExtraMinute minute) {}

    record StringWireMinuteField(@Nullable StringWireMinute minute) {}

    record PlainMinuteField(@Nullable Integer minute) {}

    record SuppressedString(
            @SuppressSchemaTypeMismatch("not needed") @Nullable String verifiedAt) {}

    record SuppressedInt(
            @SuppressSchemaTypeMismatch("dump says number") int startLine) {}

    record SuppressedInstant(
            @SuppressSchemaTypeMismatch("dump omits date-time") @Nullable Instant verifiedAt) {}

    @CatalogSchema("components/schemas/license-simple")
    record LicenseSimple(
            String key,
            String name,
            @Nullable String url,
            @Nullable String spdxId,
            String nodeId,
            @Nullable String htmlUrl) {}

    record Licensed(LicenseSimple license) {}

    record Count(int value) {
        @JsonValue
        int toJson() {
            return value;
        }
    }

    record Unrelated(String other) {}

    record CapitalRun(long userID) {}

    sealed interface Piece permits PieceA, PieceB {}

    record PieceA(@Nullable String a) implements Piece {}

    record PieceB(@Nullable String a) implements Piece {}

    record Holder(List<Piece> items) {}

    record Both(@Nullable String a, @Nullable String b) {}

    record FlatHolder(@Nullable List<Both> items) {}

    record PagesUpdate(
            @Nullable Boolean httpsEnforced,
            @Nullable @JsonProperty("public") Boolean sitePublic) {}

    record AnyOfRequired(int id, String kind, @Nullable String a) {}

    record AnyOfNullableShared(
            int id, @Nullable String kind, @Nullable String a) {}

    record AnyOfPartialRequired(int id, @Nullable String a) {}

    record Query(String q, @JsonIgnore boolean textMatch) {}

    record Scalar(String textValue, Number numberValue) {
        @JsonValue
        Object jsonValue() {
            return textValue;
        }
    }
}
