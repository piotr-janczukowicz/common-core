package io.github.piotr_janczukowicz.pagination;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * One page of a list that a server hands out in pages, and the way to the next one. {@link Pages} turns it into lazy
 * streams of items and of pages.
 *
 * <p>The page type names itself, {@code MyPage<T> implements Page<T, MyPage<T>>}, so that {@link Pages#pages(Page)}
 * returns {@code Stream<MyPage<T>>} and callers keep the accessors of their own page type.
 *
 * @param <T> type of an item
 * @param <P> the implementing page type
 */
public interface Page<T, P extends Page<T, P>> {

    /**
     * Items of this page.
     *
     * @return the items, possibly empty; an empty page is not necessarily the last one
     */
    List<T> items();

    /**
     * Loads the page after this one.
     *
     * @return the next page, or empty when this is the last one
     * @throws IOException if loading the next page fails
     */
    Optional<P> next() throws IOException;
}
