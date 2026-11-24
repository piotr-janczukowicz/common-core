package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.networknt.schema.InputFormat;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@UnitTest
class CatalogChecksExternalPropertyTest {

    private static final String HOLDER = "components/schemas/sample-holder";
    private static final String OUTER = "components/schemas/sample-outer";
    private static final String OUTER_JSON =
            "{\"holder\":{\"kind\":\"named\",\"payload\":{\"name\":\"a\"}},\"holders\":["
                    + "{\"kind\":\"named\",\"payload\":{\"name\":\"b\"}},"
                    + "{\"kind\":\"wrapped\",\"payload\":{\"url\":\"u\"}}]}";

    @Test
    void selectedBranchFailures_shouldBeEmpty_whenTheValueFitsItsBranch() {
        final SampleSupport.SampleHolder holder =
                new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("a"));

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleHolder.class,
                holder,
                "{\"kind\":\"named\",\"payload\":{\"name\":\"a\"}}",
                SampleSupport.schemaSource());

        assertThat(failures).isEmpty();
    }

    @Test
    void selectedBranchFailures_shouldUnwrapTheJsonValue_whenTheBranchIsAWrapper() {
        final SampleSupport.SampleHolder holder = new SampleSupport.SampleHolder(
                "wrapped", new SampleSupport.SampleBranchWrapped(new SampleSupport.SampleWrappedConfig("u")));

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleHolder.class,
                holder,
                "{\"kind\":\"wrapped\",\"payload\":{\"url\":\"u\"}}",
                SampleSupport.schemaSource());

        assertThat(failures).isEmpty();
    }

    @Test
    void selectedBranchFailures_shouldFail_whenThePayloadDoesNotFitTheNamedBranch() {
        final SampleSupport.SampleHolder holder =
                new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("a"));

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleHolder.class,
                holder,
                "{\"kind\":\"named\",\"payload\":{\"url\":\"u\"}}",
                SampleSupport.schemaSource());

        assertThat(failures).hasSize(1).first().asString().contains("SampleBranchNamed");
    }

    @Test
    void selectedBranchFailures_shouldNameTheClass_whenTheBranchHasNoPointer() {
        final SampleSupport.SampleHolder holder =
                new SampleSupport.SampleHolder("other", new SampleSupport.SampleBranchUnknown(Map.of()));

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleHolder.class,
                holder,
                "{\"kind\":\"other\",\"payload\":{}}",
                SampleSupport.schemaSource());

        assertThat(failures).hasSize(1).first().asString().contains("SampleBranchUnknown");
    }

    @Test
    void selectedBranchFailures_shouldBeEmpty_whenTheTypeHasNoExternalComponent() {
        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.Sample.class,
                new SampleSupport.Sample("a", 1),
                "{\"name\":\"a\",\"count\":1}",
                SampleSupport.schemaSource());

        assertThat(failures).isEmpty();
    }

    @Test
    void holderSchemaJson_shouldDropTheUnionKeyword_whenTheComponentIsExternal() {
        final String schema = SampleSupport.schemaSource().schemaJson(HOLDER);

        final String relaxed = CatalogChecks.holderSchemaJson(SampleSupport.SampleHolder.class, schema);

        assertThat(relaxed).doesNotContain("oneOf").contains("enum");
    }

    @Test
    void holderSchemaJson_shouldAcceptAnOverlappingFixture_whereTheSpecOneOfRefusesIt() {
        final String schema = SampleSupport.schemaSource().schemaJson(HOLDER);
        final String json = "{\"kind\":\"named\",\"payload\":{\"name\":\"a\"}}";

        final String relaxed = CatalogChecks.holderSchemaJson(SampleSupport.SampleHolder.class, schema);

        assertThat(CatalogChecks.compile(schema).validate(json, InputFormat.JSON))
                .isNotEmpty();
        assertThat(CatalogChecks.compile(relaxed).validate(json, InputFormat.JSON))
                .isEmpty();
    }

    @Test
    void holderSchemaJson_shouldKeepTheOtherKeywords_whenTheNodeIsANullableUnionInAnAllOf() {
        final String schema = "{\"allOf\":[{\"properties\":{\"payload\":{\"anyOf\":[{\"type\":\"null\"},"
                + "{\"description\":\"d\",\"oneOf\":[{},{}]}]}}}]}";

        final String relaxed = CatalogChecks.holderSchemaJson(SampleSupport.SampleHolder.class, schema);

        assertThat(relaxed).doesNotContain("oneOf").contains("\"null\"").contains("\"d\"");
    }

    @Test
    void holderSchemaJson_shouldFail_whenTheSchemaHasNoNodeForTheComponent() {
        final Throwable thrown = catchThrowable(
                () -> CatalogChecks.holderSchemaJson(SampleSupport.SampleHolder.class, "{\"type\":\"object\"}"));

        assertThat(thrown).isInstanceOf(AssertionError.class).hasMessageContaining("payload");
    }

    @Test
    void holderSchemaJson_shouldReturnTheSchemaUnchanged_whenTheTypeHasNoExternalComponent() {
        final String schema = SampleSupport.schemaSource().schemaJson("components/schemas/sample");

        final String result = CatalogChecks.holderSchemaJson(SampleSupport.Sample.class, schema);

        assertThat(result).isSameAs(schema);
    }

    @Test
    void selectedBranchFailures_shouldBeEmpty_whenNestedHoldersFitTheirBranches() {
        final SampleSupport.SampleOuter outer = new SampleSupport.SampleOuter(
                new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("a")),
                List.of(
                        new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("b")),
                        new SampleSupport.SampleHolder(
                                "wrapped",
                                new SampleSupport.SampleBranchWrapped(new SampleSupport.SampleWrappedConfig("u")))));

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleOuter.class, outer, OUTER_JSON, SampleSupport.schemaSource());

        assertThat(failures).isEmpty();
    }

    @Test
    void selectedBranchFailures_shouldNameTheElement_whenAListedHolderDoesNotFitItsBranch() {
        final SampleSupport.SampleOuter outer = new SampleSupport.SampleOuter(
                new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("a")),
                List.of(
                        new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("b")),
                        new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("c"))));
        final String json = "{\"holder\":{\"kind\":\"named\",\"payload\":{\"name\":\"a\"}},\"holders\":["
                + "{\"kind\":\"named\",\"payload\":{\"name\":\"b\"}},"
                + "{\"kind\":\"named\",\"payload\":{\"url\":\"u\"}}]}";

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleOuter.class, outer, json, SampleSupport.schemaSource());

        assertThat(failures).hasSize(1).first().asString().startsWith("holders[1].payload as SampleBranchNamed");
    }

    @Test
    void selectedBranchFailures_shouldNameTheNestedComponent_whenTheHolderDoesNotFitItsBranch() {
        final SampleSupport.SampleOuter outer = new SampleSupport.SampleOuter(
                new SampleSupport.SampleHolder("other", new SampleSupport.SampleBranchUnknown(Map.of())), List.of());
        final String json = "{\"holder\":{\"kind\":\"other\",\"payload\":{}},\"holders\":[]}";

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleOuter.class, outer, json, SampleSupport.schemaSource());

        assertThat(failures)
                .hasSize(1)
                .first()
                .asString()
                .startsWith("holder.payload")
                .contains("SampleBranchUnknown");
    }

    @Test
    void selectedBranchFailures_shouldSkipNullNestedComponents() {
        final SampleSupport.SampleOuterNullable outer = new SampleSupport.SampleOuterNullable(null, null);

        final List<String> failures = CatalogChecks.selectedBranchFailures(
                SampleSupport.SampleOuterNullable.class, outer, "{}", SampleSupport.schemaSource());

        assertThat(failures).isEmpty();
    }

    @Test
    void holderSchemaJson_shouldDropTheUnionKeywordOfNestedHolders_inAComponentAndInListItems() {
        final String schema = SampleSupport.schemaSource().schemaJson(OUTER);

        final String relaxed = CatalogChecks.holderSchemaJson(SampleSupport.SampleOuter.class, schema);

        assertThat(schema).contains("oneOf");
        assertThat(relaxed).doesNotContain("oneOf").contains("enum");
        assertThat(CatalogChecks.compile(schema).validate(OUTER_JSON, InputFormat.JSON))
                .isNotEmpty();
        assertThat(CatalogChecks.compile(relaxed).validate(OUTER_JSON, InputFormat.JSON))
                .isEmpty();
    }

    @Test
    void holderSchemaJson_shouldDropTheUnionKeywordOfNestedHolders_behindNullableWrappers() {
        final String schema = SampleSupport.schemaSource().schemaJson("components/schemas/sample-outer-nullable");

        final String relaxed = CatalogChecks.holderSchemaJson(SampleSupport.SampleOuterNullable.class, schema);

        assertThat(relaxed).doesNotContain("oneOf").contains("\"null\"");
        assertThat(CatalogChecks.compile(relaxed).validate("{\"holder\":null,\"holders\":null}", InputFormat.JSON))
                .isEmpty();
        assertThat(CatalogChecks.compile(relaxed).validate(OUTER_JSON, InputFormat.JSON))
                .isEmpty();
    }

    @Test
    void holderSchemaJson_shouldLeaveTheNestedSchemaStrict_whenTheNestedRecordHasNoExternalComponent() {
        final String schema = "{\"type\":\"object\",\"properties\":{\"inner\":{\"oneOf\":[{},{}]}}}";

        final String result = CatalogChecks.holderSchemaJson(SampleSupport.SamplePlainOuter.class, schema);

        assertThat(result).isSameAs(schema);
    }

    @Test
    void holderSchemaJson_shouldFail_whenTheSchemaHasNoNodeForANestedExternalComponent() {
        final String schema = "{\"type\":\"object\",\"properties\":{\"holder\":{\"type\":\"object\"}}}";

        final Throwable thrown =
                catchThrowable(() -> CatalogChecks.holderSchemaJson(SampleSupport.SampleOuter.class, schema));

        assertThat(thrown).isInstanceOf(AssertionError.class).hasMessageContaining("payload");
    }
}
