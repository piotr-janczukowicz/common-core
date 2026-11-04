package io.github.piotr_janczukowicz.json.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Writes a {@code Double} without a fractional part as a JSON integer ({@code 5.0} becomes {@code 5}). Other values,
 * including non-finite ones, negative zero (an integer cannot carry its sign) and values outside the {@code long} range
 * (such as 2^63), are written as ordinary JSON numbers.
 */
public final class IntegralDoubleSerializer extends StdSerializer<Double> {

    /** 2^63: {@code Long.MIN_VALUE} negated; exactly representable, one above {@code Long.MAX_VALUE}. */
    private static final double TWO_POW_63 = 0x1p63;

    private static final long NEGATIVE_ZERO_BITS = Double.doubleToRawLongBits(-0.0);

    public IntegralDoubleSerializer() {
        super(Double.class);
    }

    @Override
    public void serialize(final Double value, final JsonGenerator generator, final SerializationContext context) {
        final double number = value;
        if (Double.isFinite(number)
                && number >= -TWO_POW_63
                && number < TWO_POW_63
                && (long) number == number
                && Double.doubleToRawLongBits(number) != NEGATIVE_ZERO_BITS) {
            generator.writeNumber((long) number);
            return;
        }
        generator.writeNumber(number);
    }
}
