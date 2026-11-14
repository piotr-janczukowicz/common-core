package io.github.piotr_janczukowicz.test.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;

@UnitTest
class LinkHeadersTest {

    @Test
    void next_shouldBuildTheLinkValue_whenGivenAUriOrText() {
        final String fromUri = LinkHeaders.next(URI.create("https://api.example.test/items?page=2"));
        final String fromText = LinkHeaders.next("/items?page=3");

        assertSoftly(softly -> {
            softly.assertThat(fromUri).isEqualTo("<https://api.example.test/items?page=2>; rel=\"next\"");
            softly.assertThat(fromText).isEqualTo("</items?page=3>; rel=\"next\"");
        });
    }

    @Test
    void next_shouldThrowNullPointerException_whenTheTargetIsNull() {
        final Throwable noUri = catchThrowable(() -> LinkHeaders.next((URI) null));
        final Throwable noText = catchThrowable(() -> LinkHeaders.next((String) null));

        assertSoftly(softly -> {
            softly.assertThat(noUri).isInstanceOf(NullPointerException.class);
            softly.assertThat(noText).isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void constructor_shouldThrow_whenCalledThroughReflection() throws Exception {
        final Constructor<LinkHeaders> constructor = LinkHeaders.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
