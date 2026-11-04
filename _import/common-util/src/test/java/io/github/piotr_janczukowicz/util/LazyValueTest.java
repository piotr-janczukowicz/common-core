package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LazyValueTest {

    @Test
    void get_shouldCreateValueOnce_whenCalledTwice() {
        final AtomicInteger calls = new AtomicInteger();
        final LazyValue<String> lazy = new LazyValue<>(() -> "value" + calls.incrementAndGet());

        final String first = lazy.get();
        final String second = lazy.get();

        assertSoftly(softly -> {
            softly.assertThat(first).isEqualTo("value1");
            softly.assertThat(second).isSameAs(first);
            softly.assertThat(calls).hasValue(1);
        });
    }

    @Test
    void constructor_shouldThrow_whenSupplierIsNull() {
        final Throwable thrown = catchThrowable(() -> new LazyValue<>(null));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("supplier");
    }

    @Test
    void get_shouldThrow_whenSupplierReturnsNull() {
        final LazyValue<String> lazy = new LazyValue<>(() -> null);

        final Throwable thrown = catchThrowable(lazy::get);

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("value");
    }

    @Test
    void get_shouldKeepFirstValue_whenSupplierRaces() throws Exception {
        final CountDownLatch started = new CountDownLatch(2);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        final LazyValue<String> lazy = new LazyValue<>(() -> {
            calls.incrementAndGet();
            started.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("release timed out");
                }
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return "value";
        });
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final Future<String> left = pool.submit(lazy::get);
            final Future<String> right = pool.submit(lazy::get);
            final boolean isStarted = started.await(5, TimeUnit.SECONDS);
            release.countDown();

            final String leftValue = left.get();
            final String rightValue = right.get();
            final String laterValue = lazy.get();

            assertSoftly(softly -> {
                softly.assertThat(isStarted).isTrue();
                softly.assertThat(rightValue).isSameAs(leftValue);
                softly.assertThat(leftValue).isEqualTo("value");
                softly.assertThat(calls).hasValue(2);
                softly.assertThat(laterValue).isEqualTo("value");
            });
        } finally {
            pool.shutdown();
        }
    }
}
