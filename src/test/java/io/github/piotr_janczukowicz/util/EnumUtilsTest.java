package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class EnumUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(EnumUtils.class);
    }

    @ParameterizedTest
    @MethodSource("wireHits")
    void of_shouldReturnConstant_whenWireMatches(final String wire, final SampleWireEnum expected) {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final Optional<SampleWireEnum> resolved = index.of(wire);

        assertThat(resolved).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "user", ""})
    void of_shouldReturnEmpty_whenWireDoesNotMatch(final String wire) {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final Optional<SampleWireEnum> resolved = index.of(wire);

        assertThat(resolved).isEmpty();
    }

    @Test
    void index_shouldResolveEachByItsOwnMapping_whenSameEnumIndexedWithDifferentMappings() {
        final EnumUtils.ValueIndex<SampleWireEnum> byWire =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);
        final EnumUtils.ValueIndex<SampleWireEnum> byName = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::name);

        final Optional<SampleWireEnum> wireHit = byWire.of("secret");
        final Optional<SampleWireEnum> wireMiss = byWire.of("SECRET");
        final Optional<SampleWireEnum> nameHit = byName.of("SECRET");
        final Optional<SampleWireEnum> nameMiss = byName.of("secret");

        assertSoftly(softly -> {
            softly.assertThat(wireHit).contains(SampleWireEnum.SECRET);
            softly.assertThat(wireMiss).isEmpty();
            softly.assertThat(nameHit).contains(SampleWireEnum.SECRET);
            softly.assertThat(nameMiss).isEmpty();
        });
    }

    @Test
    void index_shouldThrow_whenTwoConstantsShareWireValue() {
        final Function<SampleWireEnum, String> constantWire = constant -> "same";

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(SampleWireEnum.class, constantWire));

        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessageContaining("same");
    }

    @Test
    void index_shouldApplyMappingOncePerConstant() {
        final AtomicInteger mappings = new AtomicInteger();
        final Function<SampleWireEnum, String> countingWire = constant -> {
            mappings.incrementAndGet();
            return constant.value();
        };

        EnumUtils.index(SampleWireEnum.class, countingWire);

        assertThat(mappings).hasValue(SampleWireEnum.values().length);
    }

    @Test
    void index_shouldThrowNamingConstant_whenWireValueReturnsNull() {
        final Function<SampleWireEnum, String> nullWire = constant -> null;

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(SampleWireEnum.class, nullWire));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("wire value of SECRET must not be null");
    }

    @Test
    void indexFromWireWithDefault_shouldReturnConstant_whenWireMatches() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final SampleWireEnum resolved = index.of("secret", SampleWireEnum.USER);

        assertThat(resolved).isEqualTo(SampleWireEnum.SECRET);
    }

    @Test
    void indexFromWireWithDefault_shouldReturnDefault_whenWireDoesNotMatch() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final SampleWireEnum resolved = index.of("unknown", SampleWireEnum.USER);

        assertThat(resolved).isEqualTo(SampleWireEnum.USER);
    }

    @Test
    void indexFromWireWithDefault_shouldThrow_whenDefaultValueNull() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);
        final SampleWireEnum defaultValue = null;

        final Throwable thrown = catchThrowable(() -> index.of("secret", defaultValue));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("defaultValue must not be null");
    }

    @Test
    void of_shouldThrow_whenValueNull() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);
        final String value = null;

        final Throwable thrown = catchThrowable(() -> index.of(value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("value must not be null");
    }

    @Test
    void index_shouldThrow_whenEnumClassNull() {
        final Class<SampleWireEnum> enumClass = null;

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(enumClass, SampleWireEnum::value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("enumClass must not be null");
    }

    @Test
    void index_shouldThrow_whenWireValueNull() {
        final Class<SampleWireEnum> enumClass = SampleWireEnum.class;
        final Function<SampleWireEnum, String> value = null;

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(enumClass, value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("value must not be null");
    }

    @Test
    void of_shouldResolveCorrectly_whenCalledConcurrently() throws Exception {
        final int threads = 16;
        final EnumUtils.ValueIndex<ConcurrentEnum> index = EnumUtils.index(
                ConcurrentEnum.class, constant -> constant.name().toLowerCase(Locale.ROOT));
        final CountDownLatch start = new CountDownLatch(1);
        final List<Boolean> outcomes = new ArrayList<>();
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            final List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    boolean isCorrect = true;
                    for (int round = 0; round < 200; round++) {
                        isCorrect &= index.of("one")
                                .filter(ConcurrentEnum.ONE::equals)
                                .isPresent();
                        isCorrect &= index.of("missing").isEmpty();
                    }
                    return isCorrect;
                }));
            }

            start.countDown();

            for (final Future<Boolean> result : results) {
                outcomes.add(result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(outcomes).hasSize(threads).containsOnly(true);
    }

    private static Stream<Arguments> wireHits() {
        return Stream.of(
                Arguments.of("secret", SampleWireEnum.SECRET),
                Arguments.of("User", SampleWireEnum.USER),
                Arguments.of("+1", SampleWireEnum.PLUS_ONE));
    }

    private enum ConcurrentEnum {
        ONE,
        TWO
    }

    private enum SampleWireEnum {
        SECRET("secret"),
        USER("User"),
        PLUS_ONE("+1");

        private final String value;

        SampleWireEnum(final String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    @Test
    void require_shouldReturnConstant_whenWireMatches() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final SampleWireEnum resolved = index.require("User", "sample");

        assertThat(resolved).isEqualTo(SampleWireEnum.USER);
    }

    @Test
    void require_shouldThrowWithLabelAndValue_whenWireDoesNotMatch() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final Throwable thrown = catchThrowable(() -> index.require("nope", "job status"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown job status: nope");
    }

    @Test
    void require_shouldThrowNullPointerException_whenValueIsNull() {
        final EnumUtils.ValueIndex<SampleWireEnum> index = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::value);

        final Throwable thrown = catchThrowable(() -> index.require(null, "sample"));

        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
