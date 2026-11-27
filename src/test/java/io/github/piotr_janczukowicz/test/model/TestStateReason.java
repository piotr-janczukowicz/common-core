package io.github.piotr_janczukowicz.test.model;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.piotr_janczukowicz.util.EnumUtils;

/** An enum shaped like the generated wire enums: a wire value, and a {@code of} that rejects an unknown value. */
public enum TestStateReason {
    COMPLETED("completed"),
    NOT_PLANNED("not_planned");

    private static final EnumUtils.ValueIndex<TestStateReason> VALUE_INDEX =
            EnumUtils.index(TestStateReason.class, TestStateReason::value);

    private final String value;

    TestStateReason(final String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static TestStateReason of(final String value) {
        requireNonNull(value, "value");

        return VALUE_INDEX
                .of(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown test state reason: " + value));
    }
}
