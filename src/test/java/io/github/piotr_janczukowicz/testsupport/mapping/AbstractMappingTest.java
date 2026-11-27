package io.github.piotr_janczukowicz.testsupport.mapping;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonType;
import io.github.piotr_janczukowicz.testsupport.MappingTest;
import io.github.piotr_janczukowicz.util.FileUtils;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * What a test of the JSON mapping of a type needs, and nothing else: the codec of the module under test, decoding a
 * JSON file, encoding a value, and a tree of either for comparing structure while ignoring whitespace and key order. A
 * mapping test checks a custom case of serialization or deserialization, as the catalog tests check a type against the
 * OpenAPI description, and a unit test checks the class itself.
 *
 * <p>The JSON always comes from a file: a resource next to the test class, in the same package, as with the catalog
 * fixtures. The base has no test methods; each scenario is a {@code @Test} of the subclass. A refused input is tested
 * like any other exception, with {@code catchThrowable(() -> decode("malformed.json", Type.class))}.
 */
@MappingTest
public abstract class AbstractMappingTest {

    private static final JsonMapper TREE = JsonMapper.builder().build();

    /**
     * The codec whose mapping is tested.
     *
     * @return the codec of the module under test
     */
    protected abstract JsonCodec codec();

    /**
     * Decodes a JSON file.
     *
     * @param resource the name of the JSON file, next to the test class
     * @param type the type to read
     * @param <T> the type to read
     * @return the decoded value
     * @throws NullPointerException if an argument is {@code null}
     */
    protected final <T> T decode(final String resource, final Class<T> type) {
        requireNonNull(resource, "resource");
        requireNonNull(type, "type");
        return codec().decode(read(resource), type);
    }

    /**
     * Decodes a JSON file into a generic type.
     *
     * @param resource the name of the JSON file, next to the test class
     * @param type the type to read
     * @param <T> the type to read
     * @return the decoded value
     * @throws NullPointerException if an argument is {@code null}
     */
    protected final <T> T decode(final String resource, final JsonType<T> type) {
        requireNonNull(resource, "resource");
        requireNonNull(type, "type");
        return codec().decode(read(resource), type);
    }

    /**
     * Encodes a value as JSON text.
     *
     * @param value the value to write
     * @return the JSON text
     * @throws NullPointerException if {@code value} is {@code null}
     */
    protected final String encode(final Object value) {
        requireNonNull(value, "value");
        return new String(codec().encode(value), StandardCharsets.UTF_8);
    }

    /**
     * Encodes a value and decodes the result, to show that nothing is lost on the way.
     *
     * @param value the value to write and read back
     * @param type the type to read
     * @param <T> the type of the value
     * @return the value as the codec reads it back
     * @throws NullPointerException if an argument is {@code null}
     */
    protected final <T> T roundTrip(final T value, final Class<T> type) {
        requireNonNull(value, "value");
        requireNonNull(type, "type");
        return codec().decode(codec().encode(value), type);
    }

    /**
     * Reads a JSON file as a tree, to compare with the tree of an encoded value.
     *
     * @param resource the name of the JSON file, next to the test class
     * @return the tree
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    protected final JsonNode tree(final String resource) {
        requireNonNull(resource, "resource");
        return TREE.readTree(read(resource));
    }

    /**
     * Encodes a value and reads the result as a tree.
     *
     * @param value the value to write
     * @return the tree of the JSON that the codec writes
     * @throws NullPointerException if {@code value} is {@code null}
     */
    protected final JsonNode treeOf(final Object value) {
        requireNonNull(value, "value");
        return TREE.readTree(codec().encode(value));
    }

    private byte[] read(final String resource) {
        return FileUtils.readBytes(getClass(), resource);
    }
}
