module common.json {
    requires transitive tools.jackson.databind;
    requires common.util;
    requires org.slf4j;
    requires static org.jspecify;

    exports io.github.piotr_janczukowicz.json;
    exports io.github.piotr_janczukowicz.json.jackson;
    exports io.github.piotr_janczukowicz.json.jackson.module.immutable;

    opens io.github.piotr_janczukowicz.json to
            tools.jackson.databind;
    opens io.github.piotr_janczukowicz.json.jackson to
            tools.jackson.databind;
    opens io.github.piotr_janczukowicz.json.jackson.module.immutable to
            tools.jackson.databind;
}
