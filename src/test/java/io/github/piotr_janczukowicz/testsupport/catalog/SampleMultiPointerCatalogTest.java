package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleMultiPointerCatalogTest extends AbstractCatalogTest {

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
        return SampleSupport.SampleMulti.class;
    }

    @Override
    protected boolean comparesEveryPointer() {
        return true;
    }

    @Override
    protected String fixture() {
        return "sample-multi.json";
    }

    @Override
    protected Object expected() {
        return new SampleSupport.SampleMulti("a", "b");
    }
}
