package io.github.piotr_janczukowicz.testsupport.enums.sample;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import io.github.piotr_janczukowicz.testsupport.enums.AbstractClosedStringEnumTest;

@UnitTest
class SampleClosedEnumTest extends AbstractClosedStringEnumTest<SampleClosedEnumTest.Mode> {

    SampleClosedEnumTest() {
        super(Mode.class, Mode::of, Mode::value, "unknown-value");
    }

    enum Mode {
        READ("read"),
        WRITE("write");

        private final String value;

        Mode(final String value) {
            this.value = value;
        }

        String value() {
            return value;
        }

        static Mode of(final String value) {
            if (value == null) {
                throw new NullPointerException("value");
            }
            for (final Mode mode : values()) {
                if (mode.value.equals(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown mode " + value);
        }
    }
}
