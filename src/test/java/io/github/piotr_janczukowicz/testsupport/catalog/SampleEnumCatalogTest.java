package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleEnumCatalogTest extends AbstractEnumCatalogTest {

    @Override
    protected JsonCodec codec() {
        return SampleSupport.codec();
    }

    @Override
    protected Class<?> type() {
        return SampleSupport.SampleState.class;
    }

    @Override
    protected String schema() {
        return "sample-state-schema.json";
    }
}
