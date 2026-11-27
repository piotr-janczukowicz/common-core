package io.github.piotr_janczukowicz.testsupport.enums.sample;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import io.github.piotr_janczukowicz.testsupport.enums.AbstractClosedEnumTest;

@UnitTest
class SampleIntegerEnumTest extends AbstractClosedEnumTest<SampleIntegerEnumTest.Priority, Integer> {

    SampleIntegerEnumTest() {
        super(Priority.class, Priority::of, Priority::value, -12_345);
    }

    enum Priority {
        NORMAL(0),
        URGENT(1);

        private final int value;

        Priority(final int value) {
            this.value = value;
        }

        int value() {
            return value;
        }

        static Priority of(final int value) {
            for (final Priority priority : values()) {
                if (priority.value == value) {
                    return priority;
                }
            }
            throw new IllegalArgumentException("unknown priority " + value);
        }
    }
}
