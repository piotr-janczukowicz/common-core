package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@UnitTest
class SampleLenientUnionCatalogTest extends AbstractUnionCatalogTest {

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected Class<?> type() {
        return SampleSupport.SampleLenient.class;
    }

    @Override
    protected Set<Class<?>> decodedElsewhere() {
        return Set.of(SampleSupport.SampleFallback.class);
    }

    @Override
    protected Stream<Arguments> cases() {
        return Stream.of(Arguments.of("sample-lenient-bird.json", SampleSupport.SampleBird.class));
    }
}
