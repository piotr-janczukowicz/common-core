package io.github.piotr_janczukowicz.testsupport.enums.sample;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import io.github.piotr_janczukowicz.testsupport.enums.AbstractOpenStringEnumTest;

@UnitTest
class SampleOpenEnumTest extends AbstractOpenStringEnumTest<SampleOpenEnumTest.Level> {

    SampleOpenEnumTest() {
        super(Level.class, Level::fromWire, Level::wireValue, "unknown-value", Level.UNRECOGNIZED);
    }

    enum Level {
        LOW("low"),
        HIGH("high"),
        UNRECOGNIZED("unrecognized");

        private final String wireValue;

        Level(final String wireValue) {
            this.wireValue = wireValue;
        }

        String wireValue() {
            return wireValue;
        }

        static Level fromWire(final String value) {
            if (value == null) {
                throw new NullPointerException("value");
            }
            for (final Level level : values()) {
                if (level != UNRECOGNIZED && level.wireValue.equals(value)) {
                    return level;
                }
            }
            return UNRECOGNIZED;
        }
    }
}
