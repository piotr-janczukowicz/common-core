module common.util {
    requires static org.jspecify;
    requires static io.soabase.recordbuilder.core;
    requires static java.compiler;

    exports io.github.piotr_janczukowicz.annotation;
    exports io.github.piotr_janczukowicz.openapi.model;
    exports io.github.piotr_janczukowicz.pagination;
    exports io.github.piotr_janczukowicz.util;
}
