package io.github.piotr_janczukowicz.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@UnitTest
class PagesTest {

    @Test
    void constructor_shouldRejectInstantiation_whenCalledViaReflection() throws Exception {
        final Constructor<Pages> constructor = Pages.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        final Throwable thrown = catchThrowable(constructor::newInstance);

        assertThat(thrown).hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void items_shouldReadEveryPageInOrder_whenPagesFollowEachOther() {
        final TestPage third = page(List.of("e"), unused -> Optional.empty());
        final TestPage second = page(List.of("c", "d"), unused -> Optional.of(third));
        final TestPage first = page(List.of("a", "b"), unused -> Optional.of(second));

        final List<String> items = Pages.items(first).toList();

        assertThat(items).containsExactly("a", "b", "c", "d", "e");
    }

    @Test
    void items_shouldLoadLazily_whenOnlyTheFirstItemsAreRead() {
        final AtomicInteger loads = new AtomicInteger();
        final TestPage second = page(List.of("c", "d"), unused -> Optional.empty());
        final TestPage first = page(List.of("a", "b"), unused -> {
            loads.incrementAndGet();
            return Optional.of(second);
        });

        final List<String> items = Pages.items(first).limit(2).toList();

        assertSoftly(softly -> {
            softly.assertThat(items).containsExactly("a", "b");
            softly.assertThat(loads).hasValue(0);
        });
    }

    @Test
    void itemSpliterator_shouldReportUnboundedSequentialCharacteristics() {
        final Spliterator<String> spliterator =
                Pages.items(page(List.of("a"), unused -> Optional.empty())).spliterator();

        assertSoftly(softly -> {
            softly.assertThat(spliterator.trySplit()).isNull();
            softly.assertThat(spliterator.estimateSize()).isEqualTo(Long.MAX_VALUE);
            softly.assertThat(spliterator.characteristics()).isEqualTo(Spliterator.ORDERED | Spliterator.NONNULL);
        });
    }

    @Test
    void itemSpliterator_shouldSkipEmptyPage_whenNextPageHasItems() {
        final AtomicReference<@Nullable TestPage> full = new AtomicReference<>();
        final TestPage empty = page(List.of(), current -> Optional.ofNullable(full.get()));
        full.set(page(List.of("a"), unused -> Optional.empty()));

        final List<String> items = Pages.items(empty).toList();

        assertThat(items).containsExactly("a");
    }

    @Test
    void itemSpliterator_shouldWrapFailure_whenEmptyPageNextThrows() {
        final TestPage empty = page(List.of(), unused -> {
            throw new IOException("boom");
        });

        final Throwable thrown = catchThrowable(() -> Pages.items(empty).toList());

        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasRootCauseMessage("boom");
    }

    @Test
    void pages_shouldKeepTheConcretePageType_whenPagesFollowEachOther() {
        final TestPage second = page(List.of("b"), unused -> Optional.empty());
        final TestPage first = page(List.of("a"), unused -> Optional.of(second));

        final List<TestPage> pages = Pages.pages(first).toList();

        assertThat(pages).containsExactly(first, second);
    }

    @Test
    void pageSpliterator_shouldReturnFalse_whenAdvancedAfterLastPage() {
        final Spliterator<TestPage> spliterator =
                Pages.pages(page(List.of("a"), unused -> Optional.empty())).spliterator();

        assertSoftly(softly -> {
            softly.assertThat(spliterator.tryAdvance(unused -> {})).isTrue();
            softly.assertThat(spliterator.tryAdvance(unused -> {})).isFalse();
            softly.assertThat(spliterator.tryAdvance(unused -> {})).isFalse();
        });
    }

    @Test
    void pageSpliterator_shouldWrapFailure_whenNextThrows() {
        final AtomicReference<@Nullable TestPage> seen = new AtomicReference<>();
        final TestPage first = page(List.of("a"), unused -> {
            throw new IOException("boom");
        });

        final Throwable thrown = catchThrowable(() -> Pages.pages(first).forEach(seen::set));

        assertSoftly(softly -> {
            softly.assertThat(seen.get()).isSameAs(first);
            softly.assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasRootCauseMessage("boom");
        });
    }

    @Test
    void pageSpliterator_shouldReportUnboundedSequentialCharacteristics() {
        final Spliterator<TestPage> spliterator =
                Pages.pages(page(List.of("a"), unused -> Optional.empty())).spliterator();

        assertSoftly(softly -> {
            softly.assertThat(spliterator.trySplit()).isNull();
            softly.assertThat(spliterator.estimateSize()).isEqualTo(Long.MAX_VALUE);
            softly.assertThat(spliterator.characteristics()).isEqualTo(Spliterator.ORDERED | Spliterator.NONNULL);
        });
    }

    @Test
    void all_shouldReturnEveryItem_whenPagesFollowEachOther() throws IOException {
        final TestPage second = page(List.of("b"), unused -> Optional.empty());
        final TestPage first = page(List.of("a"), unused -> Optional.of(second));

        final List<String> items = Pages.all(first);

        assertThat(items).containsExactly("a", "b");
    }

    @Test
    void all_shouldRethrowIoException_whenNextThrows() {
        final TestPage empty = page(List.of(), unused -> {
            throw new IOException("boom");
        });

        final Throwable thrown = catchThrowable(() -> Pages.all(empty));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void forEachPage_shouldVisitEveryPage_whenPagesFollowEachOther() throws IOException {
        final TestPage second = page(List.of("b"), unused -> Optional.empty());
        final TestPage first = page(List.of("a"), unused -> Optional.of(second));
        final List<TestPage> seen = new ArrayList<>();

        Pages.forEachPage(first, seen::add);

        assertThat(seen).containsExactly(first, second);
    }

    @Test
    void forEachPage_shouldRethrowIoException_whenNextThrows() {
        final TestPage first = page(List.of("a"), unused -> {
            throw new IOException("boom");
        });

        final Throwable thrown = catchThrowable(() -> Pages.forEachPage(first, unused -> {}));

        assertThat(thrown).isInstanceOf(IOException.class).hasMessage("boom");
    }

    @Test
    void checked_shouldReturnCauseWithoutSuppressedSelfReference_whenUnwrapped() {
        final IOException cause = new IOException("boom");
        final UncheckedIOException wrapper = new UncheckedIOException(cause);

        final IOException checked = Pages.checked(wrapper);

        assertSoftly(softly -> {
            softly.assertThat(checked).isSameAs(cause);
            softly.assertThat(checked.getSuppressed()).isEmpty();
        });
    }

    private static TestPage page(final List<String> items, final Next next) {
        return new TestPage(items, next);
    }

    @FunctionalInterface
    private interface Next {

        Optional<TestPage> load(TestPage current) throws IOException;
    }

    private record TestPage(List<String> items, Next loader) implements Page<String, TestPage> {

        @Override
        public Optional<TestPage> next() throws IOException {
            return loader.load(this);
        }
    }
}
