package io.github.piotr_janczukowicz.json.jackson.module.valueunion;

import io.github.piotr_janczukowicz.json.EpochUnit;
import java.time.Instant;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Writes a property marked {@link io.github.piotr_janczukowicz.json.EpochInstant} as an integer count of the unit,
 * refusing an {@link Instant} finer than the unit.
 */
final class EpochInstantSerializer extends ValueSerializer<Instant> {

    private static final int NANOS_PER_MILLI = 1_000_000;

    private final EpochUnit unit;

    EpochInstantSerializer(final EpochUnit unit) {
        super();
        this.unit = unit;
    }

    @Override
    public void serialize(final Instant value, final JsonGenerator generator, final SerializationContext context) {
        if (unit == EpochUnit.SECONDS) {
            requireWhole(value.getNano() == 0, value, generator);
            generator.writeNumber(value.getEpochSecond());
            return;
        }
        requireWhole(value.getNano() % NANOS_PER_MILLI == 0, value, generator);
        try {
            generator.writeNumber(value.toEpochMilli());
        } catch (ArithmeticException e) {
            throw DatabindException.from(generator, "Instant " + value + " does not fit in epoch milliseconds", e);
        }
    }

    private void requireWhole(final boolean whole, final Instant value, final JsonGenerator generator) {
        if (!whole) {
            throw DatabindException.from(generator, "Instant " + value + " is finer than epoch " + unit);
        }
    }
}
