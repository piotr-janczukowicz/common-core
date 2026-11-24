package io.github.piotr_janczukowicz.json;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link java.time.Instant} property whose JSON value is an integer count of {@link #value()} since
 * 1970-01-01T00:00:00Z.
 *
 * <p>Reading is lenient: the value may be a JSON integer in that unit, a number whose fraction part is zero (such as
 * {@code 5.0} or {@code 1.7E12}, read exactly), or ISO-8601 text, read as the mapper reads any {@code Instant}. JSON
 * {@code null} reads as {@code null}. A number with a non-zero fraction, any other token (boolean, object, array) and
 * an integer outside the range of {@code Instant} are refused with a Jackson {@code DatabindException}.
 *
 * <p>Writing is strict: the integer count is written. An {@code Instant} finer than the unit (non-zero nanoseconds for
 * {@link EpochUnit#SECONDS}, nanoseconds that are not a multiple of one million for {@link EpochUnit#MILLIS}) is
 * refused with a Jackson exception. A {@code null} follows the property's usual null handling. The Jackson
 * {@code ValueUnionModule} reads and writes it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface EpochInstant {

    /**
     * The unit of the integer count.
     *
     * @return the unit
     */
    EpochUnit value();
}
