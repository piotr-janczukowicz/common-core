package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.CatalogTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Round-trip and docs-schema checks of one type. The round-trip checks are final. The two schema checks can be
 * overridden only to add {@code @Disabled} with the reason, for a defect of the OpenAPI document that is not ours to
 * fix; {@code ArchitectureTest} fails an override of a test method that is not disabled, so a check cannot be emptied
 * out silently.
 *
 * <p>The encoded fixture must validate against <em>every</em> {@code @CatalogSchema} pointer of the type: several
 * pointers on one type say that the same shape sits at several nodes of the OpenAPI document. A type whose pointers
 * describe different branches of one union uses {@link AbstractXorCatalogTest}, where one pointer is enough.
 */
@CatalogTest
public abstract class AbstractCatalogTest {

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

    protected abstract String fixture();

    /**
     * The name of a stored schema file of the type, used when {@link #comparesEveryPointer()} is {@code false}.
     *
     * @return the schema resource name, or {@code null} when every pointer is compared
     */
    protected String schema() {
        return null;
    }

    /**
     * Whether the properties of the type are compared with the schema of <em>every</em> covered pointer, read through
     * {@link #schemaSource()}, instead of with the single stored file of {@link #schema()}.
     *
     * @return {@code false} by default
     */
    protected boolean comparesEveryPointer() {
        return false;
    }

    protected abstract Object expected();

    protected String[] nullableComponents() {
        return new String[0];
    }

    @Test
    protected final void decode_shouldEqualExpected_whenFixtureDecoded() {
        final Object expected = requireNonNull(expected(), "expected");
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");

        final Object decoded = codec().decode(fixtureJson, type);

        assertThat(decoded).isEqualTo(expected).hasNoNullFieldsOrPropertiesExcept(nullableComponents());
    }

    @Test
    protected final void encode_shouldMatchFixture_whenDecodedValueEncoded() {
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final byte[] encoded = codec().encode(decoded);

        assertThat(TREE.readTree(encoded)).isEqualTo(TREE.readTree(fixtureJson));
    }

    @Test
    protected void encode_shouldMatchOpenApiSchema_whenDecodedValueEncoded() {
        final List<String> pointers = CatalogChecks.jsonBodyPointers(type(), schemaSource());
        assertThat(pointers)
                .as("JSON body pointers of the @CatalogSchema annotations")
                .isNotEmpty();
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final String encoded = new String(codec().encode(decoded), StandardCharsets.UTF_8);

        final List<String> violated = pointers.stream()
                .filter(pointer -> !CatalogChecks.compile(
                                requireNonNull(schemaSource().schemaJson(pointer), "schema"))
                        .validate(encoded, InputFormat.JSON)
                        .isEmpty())
                .toList();
        assertThat(violated)
                .as("pointers whose schema the encoded fixture violates")
                .isEmpty();
    }

    @Test
    protected void type_shouldMatchDocsSchema_whenPropertiesCompared() {
        final Class<?> type = requireNonNull(type(), "type");

        final List<String> mismatches =
                CatalogChecks.docsSchemaMismatches(getClass(), type, comparesEveryPointer(), schema(), schemaSource());

        assertThat(mismatches).isEmpty();
    }

    private byte[] fixtureBytes() {
        return FileUtils.readBytes(getClass(), fixture());
    }
}
