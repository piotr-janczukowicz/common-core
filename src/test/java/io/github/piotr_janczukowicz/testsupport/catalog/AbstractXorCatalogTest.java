package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.CatalogTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Round-trip and docs-schema checks of a type whose cases are the branches of a union. The round-trip checks are final.
 * The docs-schema check can be overridden only to add {@code @Disabled} with the reason, for a defect of the OpenAPI
 * document that is not ours to fix; {@code ArchitectureTest} fails an override that is not disabled.
 *
 * <p>The encoded fixture of a case must validate against <em>at least one</em> {@code @CatalogSchema} pointer of the
 * type: the pointers of such a type sit on different branches, and a case fills exactly one of them. A type whose
 * pointers all describe the same shape uses {@link AbstractCatalogTest}, where every pointer must accept the fixture.
 */
@CatalogTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractXorCatalogTest {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    protected abstract Class<?> type();

    /**
     * The codec that decodes the fixtures and encodes the decoded values.
     *
     * @return the codec of the module under test
     */
    protected abstract JsonCodec codec();

    /**
     * Where the schema of a {@code @CatalogSchema} pointer comes from.
     *
     * @return the schema source of the module under test
     */
    protected abstract CatalogSchemaSource schemaSource();

    /**
     * The name of a stored schema file of the type, used when {@link #comparesEveryPointer()} is {@code false}.
     *
     * @return the schema resource name, or {@code null} when every pointer is compared
     */
    @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract") // optional hook: subclasses override it
    protected String schema() {
        return null;
    }

    /**
     * Whether the properties of the type are compared with the schema of <em>every</em> covered pointer, read through
     * {@link #schemaSource()}, instead of with the single stored file of {@link #schema()}. The rules for several
     * pointers are those of {@link CatalogChecks#docsSchemaMismatches(Class, List, CatalogSchemaSource)}.
     *
     * @return {@code false} by default
     */
    protected boolean comparesEveryPointer() {
        return false;
    }

    protected abstract Stream<Arguments> cases();

    @ParameterizedTest
    @MethodSource("cases")
    protected final void decode_shouldEqualExpected_whenFixtureDecoded(
            final String fixture, final Object expected, final String... exceptNullProperties) {
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(FileUtils.readBytes(getClass(), fixture), "fixture");
        requireNonNull(exceptNullProperties, "exceptNullProperties");

        final Object decoded = codec().decode(fixtureJson, type);

        assertThat(decoded).isEqualTo(requireNonNull(expected, "expected"));
        if (exceptNullProperties.length == 0) {
            assertThat(decoded).hasNoNullFieldsOrProperties();
        } else {
            assertThat(decoded).hasAllNullFieldsOrPropertiesExcept(exceptNullProperties);
        }
    }

    @ParameterizedTest
    @MethodSource("cases")
    protected final void encode_shouldMatchFixture_whenDecodedValueEncoded(final String fixture) {
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(FileUtils.readBytes(getClass(), fixture), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final byte[] encoded = codec().encode(decoded);

        assertThat(TREE.readTree(encoded)).isEqualTo(encodedMustMatch(fixture, TREE.readTree(fixtureJson)));
    }

    protected JsonNode encodedMustMatch(final String fixture, final JsonNode fixtureTree) {
        requireNonNull(fixture, "fixture");
        return fixtureTree;
    }

    @ParameterizedTest
    @MethodSource("cases")
    protected final void encode_shouldMatchOpenApiSchema_whenDecodedValueEncoded(final String fixture) {
        final List<String> pointers = CatalogChecks.jsonBodyPointers(type(), schemaSource());
        assertThat(pointers)
                .as("JSON body pointers of the @CatalogSchema annotations")
                .isNotEmpty();
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(FileUtils.readBytes(getClass(), fixture), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");
        final byte[] encoded = codec().encode(decoded);
        final List<String> failures = new ArrayList<>();
        for (final String pointer : pointers) {
            final Schema schema =
                    CatalogChecks.compile(requireNonNull(schemaSource().schemaJson(pointer), "schema"));
            final List<Error> errors = schema.validate(new String(encoded, StandardCharsets.UTF_8), InputFormat.JSON);
            if (errors.isEmpty()) {
                return;
            }
            errors.forEach(error -> failures.add(pointer + ": " + error));
        }

        assertThat(failures).as("errors per schema pointer").isEmpty();
    }

    @Test
    protected void type_shouldMatchDocsSchema_whenPropertiesCompared() {
        final Class<?> type = requireNonNull(type(), "type");

        final List<String> mismatches =
                CatalogChecks.docsSchemaMismatches(getClass(), type, comparesEveryPointer(), schema(), schemaSource());

        assertThat(mismatches).isEmpty();
    }
}
