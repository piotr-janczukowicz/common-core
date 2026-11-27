package io.github.piotr_janczukowicz.testsupport.catalog;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The decode tests of the examples an OpenAPI description gives for a type. The resource is {@code {"examples":
 * [{"from": ..., "json": ..., "refused": ...}]}}: each example is a test named by {@code from}; a {@code refused}
 * example must fail to decode, any other must decode.
 */
final class SpecExamples {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    private SpecExamples() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * The tests of one resource.
     *
     * @param owner the test class the resource name is resolved from
     * @param resource the resource name, or {@code null} when the type has no examples
     * @param type the type the examples decode to
     * @param codec the codec of the module under test
     * @return one test per example, none when {@code resource} is {@code null}
     */
    static Stream<DynamicTest> tests(
            final Class<?> owner, final String resource, final Class<?> type, final JsonCodec codec) {
        requireNonNull(owner, "owner");
        requireNonNull(type, "type");
        requireNonNull(codec, "codec");
        if (resource == null) {
            return Stream.empty();
        }
        final JsonNode examples = TREE.readTree(requireNonNull(FileUtils.readBytes(owner, resource), "resource"))
                .get("examples");
        requireNonNull(examples, "examples");
        return examples.valueStream().map(example -> {
            final String from = requireNonNull(example.get("from"), "from").asString();
            final byte[] json = TREE.writeValueAsBytes(requireNonNull(example.get("json"), "json"));
            final boolean refused = example.has("refused");
            return DynamicTest.dynamicTest(from, () -> check(from, json, refused, type, codec));
        });
    }

    private static void check(
            final String from, final byte[] json, final boolean refused, final Class<?> type, final JsonCodec codec) {
        final Object[] decoded = new Object[1];
        final Throwable thrown = catchThrowable(() -> decoded[0] = codec.decode(json, type));
        if (refused) {
            assertThat(thrown)
                    .as(from + " is marked refused, so decoding must throw")
                    .isNotNull();
        } else {
            assertThat(thrown).as(from + " must decode").isNull();
            assertThat(decoded[0]).as(from).isNotNull();
        }
    }
}
