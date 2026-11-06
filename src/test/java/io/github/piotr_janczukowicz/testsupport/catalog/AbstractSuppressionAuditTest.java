package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Fails for every {@code @SuppressSchemaTypeMismatch} that hides nothing: the Java type of the component already equals
 * what the schema says, so the annotation only silences a future difference. A module extends this class and supplies
 * the types and the schemas; the check itself lives here.
 */
@UnitTest
public abstract class AbstractSuppressionAuditTest {

    /**
     * The types to audit, usually every {@code @CatalogSchema} type of the module.
     *
     * @return the production types
     */
    protected abstract Collection<Class<?>> types();

    /**
     * Where the schema of every {@code @CatalogSchema} pointer comes from.
     *
     * @return the source of the module under test
     */
    protected abstract CatalogSchemaSource schemaSource();

    @Test
    protected final void unnecessarySuppressions_shouldBeEmpty_whenEverySuppressionHidesAMismatch() {
        final Collection<Class<?>> types = types();

        final List<String> unnecessary = CatalogChecks.unnecessarySuppressions(types, schemaSource());

        assertSoftly(softly -> {
            softly.assertThat(types).isNotEmpty();
            softly.assertThat(unnecessary)
                    .as("components whose @SuppressSchemaTypeMismatch hides nothing; remove the annotation")
                    .isEmpty();
        });
    }
}
