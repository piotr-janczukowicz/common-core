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
}
