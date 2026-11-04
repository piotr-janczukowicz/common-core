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
    void fromWire_shouldReturnConstant_whenWireMatches(final String wire, final SampleWireEnum expected) {
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);

        final Optional<SampleWireEnum> resolved = index.fromWire(wire);

        assertThat(resolved).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "user", ""})
    void fromWire_shouldReturnEmpty_whenWireDoesNotMatch(final String wire) {
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);

        final Optional<SampleWireEnum> resolved = index.fromWire(wire);

        assertThat(resolved).isEmpty();
    }

    @Test
    void index_shouldResolveEachByItsOwnMapping_whenSameEnumIndexedWithDifferentMappings() {
        final EnumUtils.WireIndex<SampleWireEnum> byWire =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);
        final EnumUtils.WireIndex<SampleWireEnum> byName = EnumUtils.index(SampleWireEnum.class, SampleWireEnum::name);

        final Optional<SampleWireEnum> wireHit = byWire.fromWire("secret");
        final Optional<SampleWireEnum> wireMiss = byWire.fromWire("SECRET");
        final Optional<SampleWireEnum> nameHit = byName.fromWire("SECRET");
        final Optional<SampleWireEnum> nameMiss = byName.fromWire("secret");

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
            return constant.wireValue();
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
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);

        final SampleWireEnum resolved = index.fromWire("secret", SampleWireEnum.USER);

        assertThat(resolved).isEqualTo(SampleWireEnum.SECRET);
    }

    @Test
    void indexFromWireWithDefault_shouldReturnDefault_whenWireDoesNotMatch() {
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);

        final SampleWireEnum resolved = index.fromWire("unknown", SampleWireEnum.USER);

        assertThat(resolved).isEqualTo(SampleWireEnum.USER);
    }

    @Test
    void indexFromWireWithDefault_shouldThrow_whenDefaultValueNull() {
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);
        final SampleWireEnum defaultValue = null;

        final Throwable thrown = catchThrowable(() -> index.fromWire("secret", defaultValue));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("defaultValue must not be null");
    }

    @Test
    void fromWire_shouldThrow_whenValueNull() {
        final EnumUtils.WireIndex<SampleWireEnum> index =
                EnumUtils.index(SampleWireEnum.class, SampleWireEnum::wireValue);
        final String value = null;

        final Throwable thrown = catchThrowable(() -> index.fromWire(value));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("value must not be null");
    }

    @Test
    void index_shouldThrow_whenEnumClassNull() {
        final Class<SampleWireEnum> enumClass = null;

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(enumClass, SampleWireEnum::wireValue));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("enumClass must not be null");
    }

    @Test
    void index_shouldThrow_whenWireValueNull() {
        final Class<SampleWireEnum> enumClass = SampleWireEnum.class;
        final Function<SampleWireEnum, String> wireValue = null;

        final Throwable thrown = catchThrowable(() -> EnumUtils.index(enumClass, wireValue));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessage("wireValue must not be null");
    }

    @Test
    void fromWire_shouldResolveCorrectly_whenCalledConcurrently() throws Exception {
        final int threads = 16;
        final EnumUtils.WireIndex<ConcurrentEnum> index = EnumUtils.index(
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
                        isCorrect &= index.fromWire("one")
                                .filter(ConcurrentEnum.ONE::equals)
                                .isPresent();
                        isCorrect &= index.fromWire("missing").isEmpty();
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

        private final String wireValue;

        SampleWireEnum(final String wireValue) {
            this.wireValue = wireValue;
        }

        String wireValue() {
            return wireValue;
        }
    }
}
