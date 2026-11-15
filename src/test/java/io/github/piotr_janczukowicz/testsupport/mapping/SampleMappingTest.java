package io.github.piotr_janczukowicz.testsupport.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonType;
import io.github.piotr_janczukowicz.json.JsonTypes;
import io.github.piotr_janczukowicz.testsupport.MappingTest;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SampleMappingTest extends AbstractMappingTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    record Sample(String name, int count) {}

    @Override
    protected JsonCodec codec() {
        return new JsonCodec() {
            @Override
            public <T> T decode(final byte[] json, final Class<T> type) {
                return MAPPER.readValue(json, type);
            }

            @Override
            public <T> T decode(final byte[] json, final JsonType<T> type) {
                return MAPPER.readValue(json, MAPPER.constructType(type.javaType()));
            }

            @Override
            public byte[] encode(final Object value) {
                return MAPPER.writeValueAsBytes(value);
            }
        };
    }

    @Test
    void decode_shouldReadTheValue_whenTheFileIsValid() {
        final Sample sample = decode("sample.json", Sample.class);

        assertThat(sample).isEqualTo(new Sample("octocat", 3));
    }

    @Test
    void decode_shouldReadAGenericType_whenAJsonTypeIsGiven() {
        final List<Sample> samples = decode("sample-list.json", JsonTypes.listOf(Sample.class));

        assertThat(samples).containsExactly(new Sample("a", 1));
    }

    @Test
    void decode_shouldRefuse_whenTheFileIsMalformed() {
        final Throwable thrown = catchThrowable(() -> decode("malformed.json", Sample.class));

        assertThat(thrown).isInstanceOf(RuntimeException.class);
    }

    @Test
    void decode_shouldRefuseNull_whenTheResourceIsNull() {
        final Throwable thrown = catchThrowable(() -> decode(null, Sample.class));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("resource");
    }

    @Test
    void encode_shouldWriteTheValue_whenTheValueIsARecord() {
        final String json = encode(new Sample("octocat", 3));

        assertThat(json).isEqualTo("{\"name\":\"octocat\",\"count\":3}");
    }

    @Test
    void encode_shouldRefuseNull_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> encode(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void roundTrip_shouldReturnAnEqualValue_whenTheValueIsARecord() {
        final Sample sample = new Sample("octocat", 3);

        final Sample result = roundTrip(sample, Sample.class);

        assertThat(result).isEqualTo(sample);
    }

    @Test
    void roundTrip_shouldRefuseNull_whenTheValueIsNull() {
        final Throwable thrown = catchThrowable(() -> roundTrip(null, Sample.class));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void tree_shouldIgnoreWhitespaceAndKeyOrder_whenTheJsonHasTheSameContent() {
        final JsonNode first = tree("sample-reordered.json");
        final JsonNode second = tree("sample.json");

        assertThat(first).isEqualTo(second);
    }

    @Test
    void treeOf_shouldReadTheEncodedValue_whenTheValueIsARecord() {
        final JsonNode expected = tree("sample.json");

        final JsonNode actual = treeOf(new Sample("octocat", 3));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void mappingTest_shouldCarryTheUnitTag_whenASubclassIsTested() {
        final Tag tag = MappingTest.class.getAnnotation(Tag.class);

        assertThat(tag.value()).isEqualTo("unit");
        assertThat(SampleMappingTest.class.isAnnotationPresent(MappingTest.class))
                .isTrue();
    }
}
