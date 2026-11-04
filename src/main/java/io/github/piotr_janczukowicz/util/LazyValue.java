package io.github.piotr_janczukowicz.util;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Thread-safe memoized supplier.
 *
 * <p>The supplier may be invoked concurrently by several threads before the first value is published; the first
 * published value wins and the results of the other invocations are discarded, so the supplier must tolerate being
 * called more than once. A supplier exception is not cached: it propagates to the caller and the next {@link #get()}
 * invokes the supplier again.
 *
 * @param <T> value type
 */
public final class LazyValue<T> {

    private final AtomicReference<@Nullable T> value = new AtomicReference<>();
    private final Supplier<T> supplier;

    /**
     * Memoizes {@code supplier}.
     *
     * @param supplier value factory
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public LazyValue(final Supplier<T> supplier) {
        this.supplier = requireNonNull(supplier, "supplier");
    }

    /**
     * Cached value, creating it on the first call.
     *
     * @return supplier result
     * @throws NullPointerException if the supplier returns {@code null}
     */
    public T get() {
        final T cached = value.get();
        return cached != null ? cached : publish();
    }

    @SuppressWarnings("java:S2637") // Sonar does not see that ValidationUtils.requireNonNull rejects null.
    private T publish() {
        final T created = requireNonNull(supplier.get(), "value");
        return value.compareAndSet(null, created) ? created : requireNonNull(value.get(), "value");
    }
}
