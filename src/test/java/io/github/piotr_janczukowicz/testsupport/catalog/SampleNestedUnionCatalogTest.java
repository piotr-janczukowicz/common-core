package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@UnitTest
class SampleNestedUnionCatalogTest extends AbstractUnionCatalogTest {

    SampleNestedUnionCatalogTest() {
        super(SampleSupport.SampleNested.class);
    }

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected Set<Class<?>> decodedElsewhere() {
        return Set.of(SampleSupport.SampleNestedCat.class, SampleSupport.SampleNestedText.class);
    }

    @Override
    protected Stream<Arguments> cases() {
        return Stream.of(Arguments.of("sample-nested-bird.json", SampleSupport.SampleNestedBird.class));
    }
}
