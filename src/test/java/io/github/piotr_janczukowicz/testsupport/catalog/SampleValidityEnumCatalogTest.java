package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleValidityEnumCatalogTest extends AbstractEnumCatalogTest {

    SampleValidityEnumCatalogTest() {
        super(SampleSupport.SampleValidity.class);
    }

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected String schema() {
        return "sample-validity-schema.json";
    }
}
