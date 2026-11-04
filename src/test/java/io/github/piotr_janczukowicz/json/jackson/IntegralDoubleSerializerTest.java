package io.github.piotr_janczukowicz.json.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.github.piotr_janczukowicz.json.JsonCodec;
import io.github.piotr_janczukowicz.json.JsonTestCodec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.json.JsonMapper;

class IntegralDoubleSerializerTest {

    private static final JsonCodec CODEC = JsonTestCodec.defaults();

    private static final JsonMapper TREE = JsonMapper.builder().build();

    @Test
    void serialize_shouldWriteInteger_whenValueIsWhole() {
        final Location location = new Location(1.0);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isTrue();
            softly.assertThat(encoded.get("line").asLong()).isEqualTo(1L);
        });
    }

    @Test
    void serialize_shouldKeepTheSign_whenValueIsNegativeZero() {
        final Location location = new Location(-0.0);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isFalse();
            softly.assertThat(encoded.get("line").asDouble()).isEqualTo(-0.0);
        });
    }

    @Test
    void serialize_shouldWriteInteger_whenValueIsPositiveZero() {
        final Location location = new Location(0.0);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isTrue();
            softly.assertThat(encoded.get("line").asLong()).isZero();
        });
    }

    @Test
    void serialize_shouldWriteDecimal_whenValueHasFraction() {
        final Location location = new Location(1.5);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isFalse();
            softly.assertThat(encoded.get("line").asDouble()).isEqualTo(1.5);
        });
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueIsNaN() {
        final Location location = new Location(Double.NaN);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertThat(encoded.get("line").isIntegralNumber()).isFalse();
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueIsInfinite() {
        final Location location = new Location(Double.POSITIVE_INFINITY);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertThat(encoded.get("line").isIntegralNumber()).isFalse();
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueExceedsLongRange() {
        final Location location = new Location(1.0e20);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertThat(encoded.get("line").isIntegralNumber()).isFalse();
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueIsBelowLongRange() {
        final Location location = new Location(-1.0e20);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertThat(encoded.get("line").isIntegralNumber()).isFalse();
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueIsExactlyTwoPow63() {
        final Location location = new Location(0x1p63);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isFalse();
            softly.assertThat(encoded.get("line").asDouble()).isEqualTo(0x1p63);
        });
    }

    @Test
    void serialize_shouldWriteInteger_whenValueIsLargestDoubleBelowTwoPow63() {
        final double largest = Math.nextDown(0x1p63);
        final Location location = new Location(largest);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isTrue();
            softly.assertThat(encoded.get("line").asLong()).isEqualTo((long) largest);
        });
    }

    @Test
    void serialize_shouldWriteInteger_whenValueIsExactlyMinusTwoPow63() {
        final Location location = new Location(-0x1p63);

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertSoftly(softly -> {
            softly.assertThat(encoded.get("line").isIntegralNumber()).isTrue();
            softly.assertThat(encoded.get("line").asLong()).isEqualTo(Long.MIN_VALUE);
        });
    }

    @Test
    void serialize_shouldWriteNonInteger_whenValueIsJustBelowMinusTwoPow63() {
        final Location location = new Location(Math.nextDown(-0x1p63));

        final byte[] json = CODEC.encode(location);
        final JsonNode encoded = TREE.readTree(json);

        assertThat(encoded.get("line").isIntegralNumber()).isFalse();
    }

    record Location(
            @JsonSerialize(using = IntegralDoubleSerializer.class)
            double line) {}
}
