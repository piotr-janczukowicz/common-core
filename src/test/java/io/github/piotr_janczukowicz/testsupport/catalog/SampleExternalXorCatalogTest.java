package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.provider.Arguments;

@UnitTest
class SampleExternalXorCatalogTest extends AbstractXorCatalogTest {

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
        return SampleSupport.SampleHolder.class;
    }

    @Override
    @Disabled("the sample holder checks the external property rule, not the docs shape")
    protected void type_shouldMatchDocsSchema_whenPropertiesCompared() {
        // disabled
    }

    @Override
    protected Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(
                        "sample-holder-empty.json",
                        new SampleSupport.SampleHolder("empty", new SampleSupport.SampleBranchEmpty("n")),
                        new String[] {}),
                Arguments.of(
                        "sample-holder-named.json",
                        new SampleSupport.SampleHolder("named", new SampleSupport.SampleBranchNamed("a")),
                        new String[] {}),
                Arguments.of(
                        "sample-holder-wrapped.json",
                        new SampleSupport.SampleHolder(
                                "wrapped",
                                new SampleSupport.SampleBranchWrapped(new SampleSupport.SampleWrappedConfig("u"))),
                        new String[] {}));
    }
}
