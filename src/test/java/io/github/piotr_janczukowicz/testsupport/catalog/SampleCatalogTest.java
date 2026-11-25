package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleCatalogTest extends AbstractCatalogTest {

    SampleCatalogTest() {
        super(SampleSupport.Sample.class, "sample.json", () -> new SampleSupport.Sample("octocat", 3));
    }

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected CatalogSchemaSource schemaSource() {
        return SampleSupport.schemaSource();
    }

    @Override
    protected String schema() {
        return "sample-schema.json";
    }
}
