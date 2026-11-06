package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@UnitTest
class SampleXorCatalogTest extends AbstractXorCatalogTest {

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected CatalogSchemaSource schemaSource() {
        return SampleSupport.schemaSource();
    }

    @Override
    protected Class<?> type() {
        return SampleSupport.SampleXor.class;
    }

    @Override
    protected String schema() {
        return "sample-xor-schema.json";
    }

    @Override
    protected Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("sample-xor-text.json", new SampleSupport.SampleXor("hello", null), new String[] {"text"}),
                Arguments.of("sample-xor-number.json", new SampleSupport.SampleXor(null, 7), new String[] {"number"}));
    }
}
