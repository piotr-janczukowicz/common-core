package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleValidityEnumCatalogTest extends AbstractEnumCatalogTest {

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected Class<?> type() {
        return SampleSupport.SampleValidity.class;
    }

    @Override
    protected String schema() {
        return "sample-validity-schema.json";
    }
}
