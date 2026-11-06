package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleCatalogTest extends AbstractCatalogTest {

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
        return SampleSupport.Sample.class;
    }

    @Override
    protected String fixture() {
        return "sample.json";
    }

    @Override
    protected String schema() {
        return "sample-schema.json";
    }

    @Override
    protected Object expected() {
        return new SampleSupport.Sample("octocat", 3);
    }
}
