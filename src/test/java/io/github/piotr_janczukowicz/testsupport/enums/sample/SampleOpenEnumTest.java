package io.github.piotr_janczukowicz.testsupport.enums.sample;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import io.github.piotr_janczukowicz.testsupport.enums.AbstractOpenStringEnumTest;

@UnitTest
class SampleOpenEnumTest extends AbstractOpenStringEnumTest<SampleOpenEnumTest.Level> {

    SampleOpenEnumTest() {
        super(Level.class, Level::of, Level::value, "unknown-value", Level.UNRECOGNIZED);
    }

    enum Level {
        LOW("low"),
        HIGH("high"),
        UNRECOGNIZED("unrecognized");

        private final String value;

        Level(final String value) {
            this.value = value;
        }

        String value() {
            return value;
        }

        static Level of(final String value) {
            if (value == null) {
                throw new NullPointerException("value");
            }
            for (final Level level : values()) {
                if (level != UNRECOGNIZED && level.value.equals(value)) {
                    return level;
                }
            }
            return UNRECOGNIZED;
        }
    }
}
