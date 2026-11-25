package io.github.piotr_janczukowicz.testsupport.enums;

import io.github.piotr_janczukowicz.testsupport.UnitTest;

@UnitTest
class SampleIntegerEnumTest extends AbstractClosedEnumTest<SampleIntegerEnumTest.Priority, Integer> {

    SampleIntegerEnumTest() {
        super(Priority.class, Priority::fromWire, Priority::wireValue, -12_345);
    }

    enum Priority {
        NORMAL(0),
        URGENT(1);

        private final int wireValue;

        Priority(final int wireValue) {
            this.wireValue = wireValue;
        }

        int wireValue() {
            return wireValue;
        }

        static Priority fromWire(final int value) {
            for (final Priority priority : values()) {
                if (priority.wireValue == value) {
                    return priority;
                }
            }
            throw new IllegalArgumentException("unknown priority " + value);
        }
    }
}
