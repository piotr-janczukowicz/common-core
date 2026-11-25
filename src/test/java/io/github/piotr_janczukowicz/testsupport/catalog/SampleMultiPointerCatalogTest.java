package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleMultiPointerCatalogTest extends AbstractCatalogTest {

    SampleMultiPointerCatalogTest() {
        super(SampleSupport.SampleMulti.class, "sample-multi.json", () -> new SampleSupport.SampleMulti("a", "b"));
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
    protected boolean comparesEveryPointer() {
        return true;
    }
}
