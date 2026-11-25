package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import com.networknt.schema.InputFormat;
import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.CatalogTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;
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
 *
 * <p>A top-level component with {@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)} is validated without the exactness
 * of its {@code oneOf}/{@code anyOf} and against the {@code @CatalogSchema} pointers of the branch class that was
 * decoded (see {@link CatalogChecks#holderSchemaJson} and {@link CatalogChecks#selectedBranchFailures}).
 */
@CatalogTest
public abstract class AbstractCatalogTest {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    private final Class<?> type;
    private final String fixture;
    private final Supplier<?> expected;

    /**
     * Creates the test of one type.
     *
     * @param type the type the fixture decodes to
     * @param fixture the name of the fixture resource, resolved from the test class
     * @param expected gives the value the fixture decodes to; called only by the decode test
     */
    protected AbstractCatalogTest(final Class<?> type, final String fixture, final Supplier<?> expected) {
        this.type = requireNonNull(type, "type");
        this.fixture = requireNonNull(fixture, "fixture");
        this.expected = requireNonNull(expected, "expected");
    }

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
     * {@link #schemaSource()}, instead of with the single stored file of {@link #schema()}.
     *
     * @return {@code false} by default
     */
    protected boolean comparesEveryPointer() {
        return false;
    }

    protected String[] nullableComponents() {
        return new String[0];
    }

    @Test
    protected final void decode_shouldEqualExpected_whenFixtureDecoded() {
        final Object expectedValue = requireNonNull(expected.get(), "expected");
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");

        final Object decoded = codec().decode(fixtureJson, type);

        assertThat(decoded).isEqualTo(expectedValue).hasNoNullFieldsOrPropertiesExcept(nullableComponents());
    }

    @Test
    protected final void encode_shouldMatchFixture_whenDecodedValueEncoded() {
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final byte[] encoded = codec().encode(decoded);

        assertThat(TREE.readTree(encoded)).isEqualTo(TREE.readTree(fixtureJson));
    }

    @Test
    protected void encode_shouldMatchOpenApiSchema_whenDecodedValueEncoded() {
        final List<String> pointers = CatalogChecks.jsonBodyPointers(type, schemaSource());
        assertThat(pointers)
                .as("JSON body pointers of the @CatalogSchema annotations")
                .isNotEmpty();
        final byte[] fixtureJson = requireNonNull(fixtureBytes(), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final String encoded = new String(codec().encode(decoded), StandardCharsets.UTF_8);

        final List<String> violated = pointers.stream()
                .filter(pointer -> !CatalogChecks.compile(CatalogChecks.holderSchemaJson(
                                type, requireNonNull(schemaSource().schemaJson(pointer), "schema")))
                        .validate(encoded, InputFormat.JSON)
                        .isEmpty())
                .toList();
        assertThat(violated)
                .as("pointers whose schema the encoded fixture violates")
                .isEmpty();
        assertThat(CatalogChecks.selectedBranchFailures(type, decoded, encoded, schemaSource()))
                .as("EXTERNAL_PROPERTY components against the branch their sibling value picked")
                .isEmpty();
    }

    @Test
    protected void type_shouldMatchDocsSchema_whenPropertiesCompared() {

        final List<String> mismatches =
                CatalogChecks.docsSchemaMismatches(getClass(), type, comparesEveryPointer(), schema(), schemaSource());

        assertThat(mismatches).isEmpty();
    }

    private byte[] fixtureBytes() {
        return FileUtils.readBytes(getClass(), fixture);
    }
}
