package io.github.piotr_janczukowicz.testsupport.catalog;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.testsupport.UnitTest;

/** A record written as a JSON array: decoded, encoded and compared with {@code prefixItems} position by position. */
@UnitTest
class SampleTupleCatalogTest extends AbstractCatalogTest {

    SampleTupleCatalogTest() {
        super(SampleSupport.SampleTuple.class, "sample-tuple.json", () -> new SampleSupport.SampleTuple(7L, "asc"));
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
        return "sample-tuple-schema.json";
    }
}
