package io.github.piotr_janczukowicz.testsupport.enums;

import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleClosedEnumTest extends AbstractClosedStringEnumTest<SampleClosedEnumTest.Mode> {

    SampleClosedEnumTest() {
        super(Mode.class, Mode::fromWire, Mode::wireValue, "unknown-value");
    }

    enum Mode {
        READ("read"),
        WRITE("write");

        private final String wireValue;

        Mode(final String wireValue) {
            this.wireValue = wireValue;
        }

        String wireValue() {
            return wireValue;
        }

        static Mode fromWire(final String value) {
            if (value == null) {
                throw new NullPointerException("value");
            }
            for (final Mode mode : values()) {
                if (mode.wireValue.equals(value)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown mode " + value);
        }
    }
}
