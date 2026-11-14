module common.core {
    requires transitive tools.jackson.databind;
    requires transitive okhttp3;
    requires okio;
    requires org.slf4j;
    requires static org.jspecify;
    requires static io.soabase.recordbuilder.core;
    requires static java.compiler;

    exports io.github.piotr_janczukowicz.annotation;
    exports io.github.piotr_janczukowicz.openapi.model;
    exports io.github.piotr_janczukowicz.pagination;
    exports io.github.piotr_janczukowicz.util;
    exports io.github.piotr_janczukowicz.json;
    exports io.github.piotr_janczukowicz.json.jackson;
    exports io.github.piotr_janczukowicz.json.jackson.module.immutable;
    exports io.github.piotr_janczukowicz.json.jackson.module.optionalnull;
    exports io.github.piotr_janczukowicz.http;
    exports io.github.piotr_janczukowicz.http.client;
    exports io.github.piotr_janczukowicz.http.client.authentication;
    exports io.github.piotr_janczukowicz.http.transport;
    exports io.github.piotr_janczukowicz.http.uri;
    exports io.github.piotr_janczukowicz.uri;

    opens io.github.piotr_janczukowicz.json to
            tools.jackson.databind;
    opens io.github.piotr_janczukowicz.json.jackson to
            tools.jackson.databind;
    opens io.github.piotr_janczukowicz.json.jackson.module.immutable to
            tools.jackson.databind;
    opens io.github.piotr_janczukowicz.json.jackson.module.optionalnull to
            tools.jackson.databind;
}
