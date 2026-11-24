package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.json.EpochInstant;
import io.github.piotr_janczukowicz.json.EpochUnit;
import io.github.piotr_janczukowicz.json.jackson.JsonMappers;
import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class EpochInstantTest {

    private static final JsonMapper MAPPER = JsonMappers.snakeCase().build();
    private static final Instant TIME = Instant.ofEpochMilli(1_700_000_000_123L);

    record Seconds(@EpochInstant(EpochUnit.SECONDS) Instant at) {}

    record Millis(@EpochInstant(EpochUnit.MILLIS) Instant at) {}

    record MaybeMillis(
            @EpochInstant(EpochUnit.MILLIS) @Nullable Instant createdAt, String name) {}

    private static Throwable readSeconds(final String json) {
        return catchThrowable(() -> MAPPER.readValue(json, Seconds.class));
    }

    private static Throwable readMillis(final String json) {
        return catchThrowable(() -> MAPPER.readValue(json, Millis.class));
    }

    @Test
    void read_shouldReadAnInteger_whenUnitIsSeconds() {
        assertThat(MAPPER.readValue("{\"at\":1700000000}", Seconds.class).at())
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
    }

    @Test
    void read_shouldReadAnInteger_whenUnitIsMillis() {
        assertThat(MAPPER.readValue("{\"at\":1700000000123}", Millis.class).at())
                .isEqualTo(TIME);
    }

    @Test
    void read_shouldReadAWholeDecimal_whenUnitIsSeconds() {
        assertThat(MAPPER.readValue("{\"at\":5.0}", Seconds.class).at()).isEqualTo(Instant.ofEpochSecond(5));
    }

    @Test
    void read_shouldReadAWholeDecimal_whenUnitIsMillis() {
        assertThat(MAPPER.readValue("{\"at\":1.7E12}", Millis.class).at())
                .isEqualTo(Instant.ofEpochMilli(1_700_000_000_000L));
        assertThat(MAPPER.readValue("{\"at\":5.0}", Millis.class).at()).isEqualTo(Instant.ofEpochMilli(5));
    }

    @Test
    void read_shouldReadIsoText_whenUnitIsSeconds() {
        assertThat(MAPPER.readValue("{\"at\":\"2023-11-14T22:13:20Z\"}", Seconds.class)
                        .at())
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
    }

    @Test
    void read_shouldReadIsoText_whenUnitIsMillis() {
        assertThat(MAPPER.readValue("{\"at\":\"2023-11-14T22:13:20.123Z\"}", Millis.class)
                        .at())
                .isEqualTo(TIME);
    }

    @Test
    void read_shouldReturnNull_whenValueIsJsonNull() {
        assertThat(MAPPER.readValue("{\"at\":null}", Millis.class).at()).isNull();
        assertThat(MAPPER.readValue("{\"at\":null}", Seconds.class).at()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.5", "0.1", "1.0E-3", "1700000000.25"})
    void read_shouldRefuseAFractionalDecimal_whenUnitIsSeconds(final String number) {
        assertThat(readSeconds("{\"at\":" + number + "}")).isInstanceOf(DatabindException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.5", "1700000000123.5", "1.0E-3"})
    void read_shouldRefuseAFractionalDecimal_whenUnitIsMillis(final String number) {
        assertThat(readMillis("{\"at\":" + number + "}")).isInstanceOf(DatabindException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "{}", "[]", "[1]"})
    void read_shouldRefuseAnyOtherToken(final String token) {
        assertThat(readSeconds("{\"at\":" + token + "}")).isInstanceOf(DatabindException.class);
        assertThat(readMillis("{\"at\":" + token + "}")).isInstanceOf(DatabindException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"99999999999999999999999", "9223372036854775807", "-9223372036854775808", "1e400"})
    void read_shouldRefuseAnOutOfRangeNumber(final String number) {
        assertThat(readSeconds("{\"at\":" + number + "}")).isInstanceOf(DatabindException.class);
    }

    @Test
    void read_shouldRefuseAnOutOfRangeNumber_whenUnitIsMillisAndNumberIsHuge() {
        assertThat(readMillis("{\"at\":99999999999999999999999}")).isInstanceOf(DatabindException.class);
        assertThat(readMillis("{\"at\":1e400}")).isInstanceOf(DatabindException.class);
    }

    @Test
    void read_shouldRefuseMalformedText() {
        assertThat(readMillis("{\"at\":\"yesterday\"}")).isInstanceOf(DatabindException.class);
    }

    @Test
    void write_shouldWriteAnInteger_whenUnitIsSeconds() {
        assertThat(MAPPER.writeValueAsString(new Seconds(Instant.ofEpochSecond(1_700_000_000L))))
                .isEqualTo("{\"at\":1700000000}");
    }

    @Test
    void write_shouldWriteAnInteger_whenUnitIsMillis() {
        assertThat(MAPPER.writeValueAsString(new Millis(TIME))).isEqualTo("{\"at\":1700000000123}");
    }

    @Test
    void write_shouldWriteNegativeCounts_whenInstantIsBeforeTheEpoch() {
        assertThat(MAPPER.writeValueAsString(new Millis(Instant.ofEpochMilli(-1500))))
                .isEqualTo("{\"at\":-1500}");
    }

    @Test
    void write_shouldRefuseFinerPrecision_whenUnitIsSeconds() {
        final Throwable thrown =
                catchThrowable(() -> MAPPER.writeValueAsString(new Seconds(Instant.ofEpochSecond(5, 1_000_000))));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("finer than epoch SECONDS");
    }

    @Test
    void write_shouldRefuseFinerPrecision_whenUnitIsMillis() {
        final Throwable thrown =
                catchThrowable(() -> MAPPER.writeValueAsString(new Millis(Instant.ofEpochSecond(5, 1_000_001))));

        assertThat(thrown).isInstanceOf(DatabindException.class).hasMessageContaining("finer than epoch MILLIS");
    }

    @Test
    void write_shouldRefuseAnInstantBeyondTheMillisecondRange() {
        final Throwable thrown = catchThrowable(
                () -> MAPPER.writeValueAsString(new Millis(Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1))));

        assertThat(thrown).isInstanceOf(DatabindException.class);
    }

    @Test
    void write_shouldWriteNull_whenComponentIsNull() {
        assertThat(MAPPER.writeValueAsString(new MaybeMillis(null, "x"))).contains("\"created_at\":null");
    }

    @Test
    void roundTrip_shouldWorkThroughSnakeCaseMapper_whenComponentIsNullable() {
        final MaybeMillis original = new MaybeMillis(TIME, "x");

        final String json = MAPPER.writeValueAsString(original);

        assertThat(json).isEqualTo("{\"created_at\":1700000000123,\"name\":\"x\"}");
        assertThat(MAPPER.readValue(json, MaybeMillis.class)).isEqualTo(original);
        assertThat(MAPPER.readValue("{\"name\":\"x\"}", MaybeMillis.class).createdAt())
                .isNull();
    }

    @Test
    void module_shouldReportItsVersionThroughTheIntrospector() {
        assertThat(new ValueUnionModule.EpochInstantIntrospector().version())
                .isEqualTo(new ValueUnionModule().version());
    }
}
