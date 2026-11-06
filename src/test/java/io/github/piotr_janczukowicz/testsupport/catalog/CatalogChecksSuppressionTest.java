package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.openapi.model.CatalogSchema;
import io.github.piotr_janczukowicz.openapi.model.SuppressSchemaTypeMismatch;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class CatalogChecksSuppressionTest {

    @CatalogSchema("components/schemas/sample-text")
    record NeedlessSuppression(
            @SuppressSchemaTypeMismatch("not needed") String text) {}

    @CatalogSchema("components/schemas/sample-number")
    record NeededSuppression(
            @SuppressSchemaTypeMismatch("docs say integer") double number) {}

    @CatalogSchema("components/schemas/sample-text")
    record WithoutSuppression(String text) {}

    @Test
    void unnecessarySuppressions_shouldListTheComponent_whenItsTypeAlreadyMatches() {
        final List<Class<?>> types = List.of(NeedlessSuppression.class);

        final List<String> unnecessary = CatalogChecks.unnecessarySuppressions(types, SampleSupport.schemaSource());

        assertThat(unnecessary).containsExactly(NeedlessSuppression.class.getName() + ".text");
    }

    @Test
    void unnecessarySuppressions_shouldBeEmpty_whenTheSuppressionHidesAMismatchOrThereIsNone() {
        final List<Class<?>> types = List.of(NeededSuppression.class, WithoutSuppression.class);

        final List<String> unnecessary = CatalogChecks.unnecessarySuppressions(types, SampleSupport.schemaSource());

        assertThat(unnecessary).isEmpty();
    }
}
