package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@UnitTest
class SampleUnionCatalogTest extends AbstractUnionCatalogTest {

    SampleUnionCatalogTest() {
        super(SampleSupport.SampleUnion.class);
    }

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("sample-union-cat.json", SampleSupport.SampleCat.class),
                Arguments.of("sample-union-dog.json", SampleSupport.SampleDog.class));
    }

    @Override
    protected String specExamples() {
        return "sample-union.examples.json";
    }
}
