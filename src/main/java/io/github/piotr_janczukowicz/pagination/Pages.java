package io.github.piotr_janczukowicz.pagination;

import java.io.IOException;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

/**
 * Sequential lazy streams over a walk of {@link Page}s. A page is loaded only when the stream needs an item from it, so
 * {@code limit(n)} stops further requests.
 */
public final class Pages {

    private Pages() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Items of {@code first} and of every page after it, in order.
     *
     * <p>A failure to load a page surfaces as an {@link UncheckedIOException} from the stream operation.
     *
     * @param first first page
     * @return lazy sequential stream of items
     * @param <T> type of an item
     * @param <P> page type
     */
    public static <T, P extends Page<T, P>> Stream<T> items(final P first) {
        return StreamSupport.stream(new ItemSpliterator<>(first), false);
    }

    /**
     * {@code first} and every page after it, in order, each as the page type of the caller.
     *
     * <p>A failure to load a page surfaces as an {@link UncheckedIOException} from the stream operation.
     *
     * @param first first page
     * @return lazy sequential stream of pages
     * @param <T> type of an item
     * @param <P> page type
     */
    public static <T, P extends Page<T, P>> Stream<P> pages(final P first) {
        return StreamSupport.stream(new PageSpliterator<>(first), false);
    }

    /**
     * Every item of {@code first} and of the pages after it.
     *
     * @param first first page
     * @return all items, read eagerly
     * @param <T> type of an item
     * @param <P> page type
     * @throws IOException if loading a page fails
     */
    public static <T, P extends Page<T, P>> List<T> all(final P first) throws IOException {
        try {
            return Pages.<T, P>items(first).toList();
        } catch (final WalkFailure ex) {
            throw checked(ex);
        }
    }

    /**
     * Passes {@code first} and every page after it to {@code consumer}, in order.
     *
     * @param first first page
     * @param consumer receives each page; a failure it throws is not an I/O failure of the walk and is left alone
     * @param <T> type of an item
     * @param <P> page type
     * @throws IOException if loading a page fails
     */
    public static <T, P extends Page<T, P>> void forEachPage(final P first, final Consumer<P> consumer)
            throws IOException {
        try {
            Pages.<T, P>pages(first).forEach(consumer);
        } catch (final WalkFailure ex) {
            throw checked(ex);
        }
    }

    static IOException checked(final UncheckedIOException failure) {
        return failure.getCause();
    }

    /** I/O failure of the page walk itself; one thrown by a consumer is not this type and is left alone. */
    private static final class WalkFailure extends UncheckedIOException {

        @Serial
        private static final long serialVersionUID = 1L;

        WalkFailure(final IOException cause) {
            super(cause);
        }
    }

    private static final class ItemSpliterator<T, P extends Page<T, P>> implements Spliterator<T> {

        private @Nullable P page;
        private int index;

        private ItemSpliterator(final P first) {
            this.page = first;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            while (page != null) {
                final List<T> current = page.items();
                if (index < current.size()) {
                    action.accept(current.get(index));
                    index++;
                    return true;
                }
                try {
                    page = page.next().orElse(null);
                } catch (final IOException ex) {
                    throw new WalkFailure(ex);
                }
                index = 0;
            }
            return false;
        }

        @Override
        public @Nullable Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL;
        }
    }

    private static final class PageSpliterator<T, P extends Page<T, P>> implements Spliterator<P> {

        private P page;
        private boolean isReady;
        private boolean isFinished;

        private PageSpliterator(final P first) {
            this.page = first;
            this.isReady = true;
        }

        @Override
        public boolean tryAdvance(final Consumer<? super P> action) {
            if (isFinished) {
                return false;
            }
            if (isReady) {
                action.accept(page);
                isReady = false;
                return true;
            }
            try {
                final Optional<P> following = page.next();
                if (following.isEmpty()) {
                    isFinished = true;
                    return false;
                }
                page = following.get();
                action.accept(page);
                return true;
            } catch (final IOException ex) {
                throw new WalkFailure(ex);
            }
        }

        @Override
        public @Nullable Spliterator<P> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | NONNULL;
        }
    }
}
