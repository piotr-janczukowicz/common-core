package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class LinkHeaderTest {

    static Stream<URI> bases() {
        return Stream.of(
                URI.create("https://api.example.test/"), URI.create("https://host.example.test/gitlab/api/v4/"));
    }

    @ParameterizedTest
    @MethodSource("bases")
    void parse_shouldExtractNext_whenHeaderListsSeveralRelations(final URI base) {
        final LinkHeader links = LinkHeader.parse("<" + base.resolve("user?page=1") + ">; rel=\"prev\", "
                + "<" + base.resolve("user?page=3") + ">; rel=\"next\", "
                + "<" + base.resolve("user?page=1") + ">; rel=\"first\", "
                + "<" + base.resolve("user?page=5") + ">; rel=\"last\"");

        assertThat(links.next()).contains(base.resolve("user?page=3"));
    }

    @Test
    void parse_shouldIgnoreMalformedEntries_whenHeaderInvalid() {
        final LinkHeader links = LinkHeader.parse("not-a-link, <https://api.example.test/user>; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user"));
    }

    @Test
    void parse_shouldExtractNext_whenRelListsSeveralNames() {
        final LinkHeader links = LinkHeader.parse("<https://api.example.test/user?page=2>; rel=\"prev next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?page=2"));
    }

    @Test
    void parse_shouldSkipEntry_whenRelIsOnlyWhitespace() {
        final LinkHeader links = LinkHeader.parse(
                "<https://api.example.test/user>; rel=\"   \", <https://api.example.test/user?page=2>; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?page=2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"rel=\"next\"", "rel=next", "rel=\"Next\"", "REL=\"next\"", "rel = next"})
    void parse_shouldExtractNext_whenRelQuotedUnquotedOrMixedCase(final String rel) {
        final LinkHeader links = LinkHeader.parse("<https://api.example.test/user?page=2>; " + rel);

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?page=2"));
    }

    @Test
    void parse_shouldThrow_whenNextTargetIsNotValidUri() {
        final Throwable thrown = catchThrowable(() -> LinkHeader.parse(
                "<https://user:pw@api.example.test/search?q=a b&access_token=secret#frag>; rel=\"next\", "
                        + "<https://api.example.test/user?page=2>; rel=\"next\""));

        assertThat(thrown)
                .isInstanceOf(PaginationException.class)
                .hasMessageContaining("https://api.example.test/search")
                .hasMessageNotContaining("secret")
                .hasMessageNotContaining("user:pw")
                .hasMessageNotContaining("frag");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
                    https://host/a b             | https://host/a b
                    https://host a               | https://host a
                    https://host/p q#frag        | https://host/p q
                    https://host/p q?x=secret    | https://host/p q
                    https://u:pw@host:8443/p q   | https://host:8443/p q
                    1x://host/p q                | <not a URI>
                    ://host/p q                  | <not a URI>
                    """)
    void parse_shouldDescribeOnlySchemeHostAndPath_whenNextTargetInvalid(
            final String target, final String description) {
        final Throwable thrown = catchThrowable(() -> LinkHeader.parse("<" + target + ">; rel=next"));

        assertThat(thrown).isInstanceOf(PaginationException.class).hasMessageEndingWith(": " + description);
    }

    @Test
    void parse_shouldDescribeGenerically_whenInvalidNextTargetHasNoScheme() {
        final Throwable thrown = catchThrowable(() -> LinkHeader.parse("<a b?token=secret>; rel=\"next\""));

        assertThat(thrown)
                .isInstanceOf(PaginationException.class)
                .hasMessageContaining("<not a URI>")
                .hasMessageNotContaining("secret");
    }

    @Test
    void parse_shouldSkipSilently_whenOtherRelationHasInvalidUri() {
        final LinkHeader links =
                LinkHeader.parse("<http://[>; rel=\"prev\", <https://api.example.test/user?page=2>; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?page=2"));
    }

    @Test
    void parse_shouldIgnoreRelInsideQuotedParameter_whenTitleMentionsNext() {
        final LinkHeader links = LinkHeader.parse(
                "<https://api.example.test/a>; title=\"x; rel=next\"; rel=\"prev\", <https://api.example.test/b>; rel=next");

        assertThat(links.next()).contains(URI.create("https://api.example.test/b"));
    }

    @Test
    void parse_shouldExtractNext_whenEarlierEntryHasUnbalancedQuote() {
        final LinkHeader links = LinkHeader.parse(
                "<https://api.example.test/a>; title=\"x, <https://api.example.test/x?page=2>; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/x?page=2"));
    }

    @Test
    void parse_shouldExtractNext_whenUnquotedRelIsCommaTerminated() {
        final LinkHeader links =
                LinkHeader.parse("<https://api.example.test/u>; rel=next, " + "<https://api.example.test/v>; rel=last");

        assertThat(links.next()).contains(URI.create("https://api.example.test/u"));
    }

    @Test
    void parse_shouldNotSplitEntry_whenOpeningBracketIsInsideQuotedParameter() {
        final LinkHeader links = LinkHeader.parse("<https://api.example.test/a>; title=\"a <b\"; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/a"));
    }

    @Test
    void parse_shouldExtractNext_whenRelIsNotFirstParameter() {
        final LinkHeader links =
                LinkHeader.parse("<https://api.example.test/user?page=2>; title=\"x\"; type=\"a,b\"; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?page=2"));
    }

    @Test
    void parse_shouldNotSplitEntry_whenCommaIsInsideUrl() {
        final LinkHeader links = LinkHeader.parse(
                "<https://api.example.test/user?a=1,2>; rel=\"next\", <https://api.example.test/user?page=1>; rel=\"prev\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/user?a=1,2"));
    }

    @Test
    void parse_shouldUseFirstNextEntry_whenSeveralEntriesNameNext() {
        final LinkHeader links = LinkHeader.parse(
                "<https://api.example.test/a>; rel=\"next\", <https://api.example.test/b>; rel=\"next\"");

        assertThat(links.next()).contains(URI.create("https://api.example.test/a"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "<https://api.example.test/user?page=1>; rel=\"prev\", <https://api.example.test/user?page=1>; rel=\"first\"",
                "<https://api.example.test/user?page=2>; rel=prev; rel=next",
                "<https://api.example.test/a>; title=\"x; rel=next\"",
                "<https://api.example.test/a>; rel",
                "<https://api.example.test/a>; rel=",
                "<https://api.example.test/a>;",
                "<https://api.example.test/a>; title=x, ",
                "<https://api.example.test/a>; title=x, <https://api.example.test/b",
                "<https://api.example.test/a>; rel=nextish"
            })
    void parse_shouldYieldNoNext_whenHeaderHasNoUsableNextRelation(final String header) {
        assertThat(LinkHeader.parse(header).next()).isEmpty();
    }

    @Test
    void parse_shouldThrow_whenHeaderValueNull() {
        final Throwable thrown = catchThrowable(() -> LinkHeader.parse(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("headerValue");
    }

    @Test
    void parse_shouldThrow_whenNextTargetHasUnencodedPipe() {
        final Throwable thrown = catchThrowable(
                () -> LinkHeader.parse("<https://api.example.test/search?q=a|b&access_token=secret>; rel=\"next\""));

        assertThat(thrown)
                .isInstanceOf(PaginationException.class)
                .hasMessageContaining("https://api.example.test/search")
                .hasMessageNotContaining("secret");
    }
}
