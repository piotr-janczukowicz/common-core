package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.net.URI;
import org.junit.jupiter.api.Test;

@UnitTest
class PageWalkGuardTest {

    private static final URI FIRST = URI.create("https://api.example.test/items?per_page=20&page=1");
    private static final URI SECOND = URI.create("https://api.example.test/items?per_page=20&page=2");

    @Test
    void requireNext_shouldAccept_whenLinkIsNew() {
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 1));

        assertThat(thrown).isNull();
    }

    @Test
    void requireNext_shouldThrowLoop_whenLinkPointsBackAtTheFirstRequest() {
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(FIRST, 1));

        assertThat(thrown).isInstanceOf(PaginationLoopException.class).hasMessageContaining("1 page(s)");
    }

    @Test
    void requireNext_shouldThrowLoop_whenLinkWasFollowedBefore() {
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);
        guard.markFollowed(SECOND);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 2));

        assertThat(thrown).isInstanceOf(PaginationLoopException.class);
    }

    @Test
    void requireNext_shouldTreatQueryParametersInAnotherOrderAsTheSamePage() {
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);
        final URI reordered = URI.create("https://api.example.test/items?page=1&per_page=20");

        final Throwable thrown = catchThrowable(() -> guard.requireNext(reordered, 1));

        assertThat(thrown).isInstanceOf(PaginationLoopException.class);
    }

    @Test
    void requireNext_shouldNotCountALinkAsFollowed_whenItWasOnlyChecked() {
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);
        guard.requireNext(SECOND, 1);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 1));

        assertThat(thrown).isNull();
    }

    @Test
    void requireNext_shouldThrowLimit_whenMaxPagesWereRead() {
        final PageWalkGuard guard = new PageWalkGuard(2, FIRST);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 2));

        assertThat(thrown)
                .isInstanceOfSatisfying(
                        PaginationLimitExceededException.class,
                        limit -> assertSoftly(softly -> {
                            softly.assertThat(limit.maxPages()).isEqualTo(2);
                            softly.assertThat(limit.pageCount()).isEqualTo(2);
                        }));
    }

    @Test
    void requireNext_shouldAccept_whenFewerThanMaxPagesWereRead() {
        final PageWalkGuard guard = new PageWalkGuard(2, FIRST);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 1));

        assertThat(thrown).isNull();
    }

    @Test
    void requireNext_shouldCheckTheLimitBeforeTheLoop_whenBothApply() {
        final PageWalkGuard guard = new PageWalkGuard(1, FIRST);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(FIRST, 1));

        assertThat(thrown).isInstanceOf(PaginationLimitExceededException.class);
    }

    @Test
    void requireNext_shouldNotQuoteTheQuery_whenLoopIsReported() {
        final URI link = URI.create("https://api.example.test/items?token=secret");
        final PageWalkGuard guard = new PageWalkGuard(null, link);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(link, 1));

        assertThat(thrown).isInstanceOf(PaginationLoopException.class).hasMessageNotContaining("secret");
    }

    @Test
    void requireNext_shouldThrowLoop_whenLinkWithoutQueryRepeats() {
        final URI link = URI.create("https://api.example.test/items/next");
        final PageWalkGuard guard = new PageWalkGuard(null, FIRST);
        guard.markFollowed(link);

        final Throwable thrown = catchThrowable(() -> guard.requireNext(link, 2));

        assertThat(thrown).isInstanceOf(PaginationLoopException.class);
    }

    @Test
    void requireNext_shouldNotConfuseLinksThatDifferOnlyByQuery_whenOneHasNone() {
        final PageWalkGuard guard = new PageWalkGuard(null, URI.create("https://api.example.test/items"));

        final Throwable thrown = catchThrowable(() -> guard.requireNext(SECOND, 1));

        assertThat(thrown).isNull();
    }

    @Test
    void constructor_shouldRefuse_whenMaxPagesBelowOne() {
        final Throwable thrown = catchThrowable(() -> new PageWalkGuard(0, FIRST));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }
}
