package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class PageLinksTest {

    private static final URI BASE = URI.create("https://gitlab.example.test/api/v4/");
    private static final URI CURRENT = URI.create("https://gitlab.example.test/api/v4/projects?page=1");

    @Test
    void absolute_shouldReturnTheLink_whenItIsAbsolute() {
        final URI link = URI.create("https://gitlab.example.test/api/v4/projects?page=2");

        final URI absolute = PageLinks.absolute(CURRENT, link);

        assertThat(absolute).isSameAs(link);
    }

    @Test
    void absolute_shouldResolveAgainstTheRequest_whenTheLinkIsRelative() {
        final URI absolute = PageLinks.absolute(CURRENT, URI.create("/api/v4/projects?page=2"));

        assertThat(absolute).isEqualTo(URI.create("https://gitlab.example.test/api/v4/projects?page=2"));
    }

    @Test
    void absolute_shouldThrowPaginationExceptionWithoutTheQuery_whenTheLinkCannotBeResolved() {
        final URI link = URI.create("//host:99999/api/v4/projects?page=2&token=secret");

        final Throwable thrown = catchThrowable(() -> PageLinks.absolute(CURRENT, link));

        assertThat(thrown)
                .isInstanceOf(PaginationException.class)
                .hasMessageContaining("cannot be resolved")
                .hasMessageNotContaining("secret")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "https://other.example.test/api/v4/projects?page=2, true",
        "http://gitlab.example.test/api/v4/projects?page=2, true",
        "https://gitlab.example.test:8443/api/v4/projects, true",
        "https://gitlab.example.test/api/v4/projects?page=2, false",
        "https://other.example.test/elsewhere/projects, false",
        "https://my_host/api/v4/projects, false",
        "/api/v4/projects?page=2, false"
    })
    void differsOnlyByOrigin_shouldTellWhetherOnlyTheOriginDiffers_whenTheLinkIsGiven(
            final String link, final boolean expected) {
        final boolean differs = PageLinks.differsOnlyByOrigin(BASE, URI.create(link));

        assertThat(differs).isEqualTo(expected);
    }

    @Test
    void differsOnlyByOrigin_shouldThrowNullPointerException_whenAnArgumentIsNull() {
        final Throwable noBase = catchThrowable(() -> PageLinks.differsOnlyByOrigin(null, CURRENT));
        final Throwable noLink = catchThrowable(() -> PageLinks.differsOnlyByOrigin(BASE, null));
        final Throwable noCurrent = catchThrowable(() -> PageLinks.absolute(null, CURRENT));
        final Throwable noTarget = catchThrowable(() -> PageLinks.absolute(CURRENT, null));

        assertSoftly(softly -> {
            softly.assertThat(noBase).isInstanceOf(NullPointerException.class).hasMessageContaining("base");
            softly.assertThat(noLink).isInstanceOf(NullPointerException.class).hasMessageContaining("link");
            softly.assertThat(noCurrent)
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("currentRequest");
            softly.assertThat(noTarget).isInstanceOf(NullPointerException.class).hasMessageContaining("link");
        });
    }

    @Test
    void constructor_shouldThrow_whenCalledThroughReflection() throws Exception {
        final Constructor<PageLinks> constructor = PageLinks.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
