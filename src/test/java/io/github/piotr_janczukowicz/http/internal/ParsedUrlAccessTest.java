package io.github.piotr_janczukowicz.http.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.http.uri.ParsedUrl;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;

class ParsedUrlAccessTest {

    @Test
    void okHttpUrl_shouldExposeTheParsedUrl() {
        final ParsedUrl url = ParsedUrl.of(URI.create("https://api.example.test/a?x=1"));

        assertThat(ParsedUrlAccess.okHttpUrl(url)).hasToString("https://api.example.test/a?x=1");
    }

    @Test
    void okHttpUrl_shouldThrowNullPointer_whenUrlNull() {
        final Throwable thrown = catchThrowable(() -> ParsedUrlAccess.okHttpUrl(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("url must not be null");
    }

    @Test
    void register_shouldThrowNullPointer_whenFunctionNull() {
        final Throwable thrown = catchThrowable(() -> ParsedUrlAccess.register(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("function must not be null");
    }

    @Test
    void constructor_shouldThrowUnsupported_whenCalledViaReflection() throws Exception {
        final Constructor<ParsedUrlAccess> constructor = ParsedUrlAccess.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
