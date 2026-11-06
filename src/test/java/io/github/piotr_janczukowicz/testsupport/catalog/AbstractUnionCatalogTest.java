package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.CatalogTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks of a sealed interface whose subtypes are told apart by Jackson ({@code @JsonSubTypes}): a fixture decoded as
 * the interface must give the expected subtype, encoding it must give the fixture back, and the cases must cover every
 * permitted subtype. The checks are final.
 *
 * <p>The cases of a union with a fallback include a payload that only the fallback matches, so the test shows which
 * subtype wins when a payload is ambiguous.
 */
@CatalogTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractUnionCatalogTest {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    /**
     * The sealed interface that is decoded.
     *
     * @return the union type
     */
    protected abstract Class<?> type();

    /**
     * The codec that decodes the fixtures and encodes the decoded values.
     *
     * @return the codec of the module under test
     */
    protected abstract JsonCodec codec();

    /**
     * The cases: a fixture resource name (resolved from the test class, so an absolute name reaches another package)
     * and the subtype that decoding it must give.
     *
     * @return {@code Arguments.of(String fixture, Class<?> subtype)} per case
     */
    protected abstract Stream<Arguments> cases();

    /**
     * Permitted subtypes that Jackson never builds from the union itself, because a custom deserializer of an enclosing
     * type creates them. They need no case.
     *
     * @return the subtypes left out of the coverage check, none by default
     */
    protected Set<Class<?>> decodedElsewhere() {
        return Set.of();
    }

    @ParameterizedTest
    @MethodSource("cases")
    protected final void decode_shouldReturnTheSubtype_whenFixtureDecoded(
            final String fixture, final Class<?> subtype) {
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(FileUtils.readBytes(getClass(), fixture), "fixture");

        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        assertThat(decoded.getClass()).as(fixture).isEqualTo(subtype);
    }

    @ParameterizedTest
    @MethodSource("cases")
    protected final void encode_shouldMatchFixture_whenDecodedValueEncoded(
            final String fixture, final Class<?> subtype) {
        final Class<?> type = requireNonNull(type(), "type");
        final byte[] fixtureJson = requireNonNull(FileUtils.readBytes(getClass(), fixture), "fixture");
        final Object decoded = requireNonNull(codec().decode(fixtureJson, type), "decoded");

        final byte[] encoded = codec().encode(decoded);

        assertThat(TREE.readTree(encoded))
                .as(fixture + " as " + subtype.getSimpleName())
                .isEqualTo(TREE.readTree(fixtureJson));
    }

    @Test
    protected final void cases_shouldCoverEveryPermittedSubtype() {
        final Class<?> type = requireNonNull(type(), "type");
        assertThat(type.isSealed()).as(type.getTypeName() + " is sealed").isTrue();
        final Set<Class<?>> covered = cases().map(
                        arguments -> (Class<?>) arguments.get()[1])
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final List<Class<?>> permitted = List.of(type.getPermittedSubclasses());

        final Set<Class<?>> expected = new LinkedHashSet<>(permitted);
        expected.removeAll(decodedElsewhere());

        assertSoftly(softly -> {
            softly.assertThat(covered).as("covered subtypes are permitted").isSubsetOf(permitted);
            softly.assertThat(covered).as("permitted subtypes are covered").containsAll(expected);
        });
    }
}
