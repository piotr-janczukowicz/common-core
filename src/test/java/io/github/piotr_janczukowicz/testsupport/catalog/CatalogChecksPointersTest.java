package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class CatalogChecksPointersTest {

    private static final String A = "components/schemas/sample-multi-a";
    private static final String B = "components/schemas/sample-multi-b";

    @Test
    void docsSchemaMismatches_shouldBeEmpty_whenTheTypeMatchesEveryPointer() {
        final List<String> mismatches = CatalogChecks.docsSchemaMismatches(
                SampleSupport.SampleMulti.class, List.of(A, B), SampleSupport.schemaSource());

        assertThat(mismatches).isEmpty();
    }

    @Test
    void docsSchemaMismatches_shouldReturnTheDifferences_whenOnePointerAndTheTypeHasAnExtraProperty() {
        final List<String> mismatches = CatalogChecks.docsSchemaMismatches(
                SampleSupport.SampleMulti.class, List.of(B), SampleSupport.schemaSource());

        assertThat(mismatches).hasSize(1).first().asString().contains("extra");
    }

    @Test
    void docsSchemaMismatches_shouldReportAPointerProperty_whenTheTypeLacksIt() {
        final List<String> mismatches = CatalogChecks.docsSchemaMismatches(
                SampleSupport.Sample.class, List.of(A, "components/schemas/sample"), SampleSupport.schemaSource());

        assertThat(mismatches).anyMatch(difference -> difference.startsWith(A) && difference.contains("extra"));
    }

    @Test
    void docsSchemaMismatches_shouldReportAJavaProperty_whenNoPointerHasIt() {
        final List<String> mismatches = CatalogChecks.docsSchemaMismatches(
                SampleSupport.SampleMulti.class,
                List.of(B, "components/schemas/sample-text"),
                SampleSupport.schemaSource());

        assertThat(mismatches)
                .anyMatch(difference -> difference.startsWith("no pointer has it") && difference.contains("extra"));
    }

    @Test
    void docsSchemaMismatches_shouldAllowANullableComponent_whenOnlySomePointersRequireIt() {
        final List<String> mismatches = CatalogChecks.docsSchemaMismatches(
                SampleSupport.SampleMulti.class, List.of(A, B), SampleSupport.schemaSource());

        assertThat(mismatches).noneMatch(difference -> difference.contains("name"));
    }
}
