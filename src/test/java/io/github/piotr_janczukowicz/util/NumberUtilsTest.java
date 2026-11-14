package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NumberUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(NumberUtils.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"42", " 42 ", "\t42\n"})
    void parseInt_shouldReturnNumber_whenTextIsAWholeNumber(final String text) {
        final OptionalInt number = NumberUtils.parseInt(text);

        assertThat(number).hasValue(42);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "-", "4x", "4.5", "+4", "٤٢", "99999999999"})
    void parseInt_shouldReturnEmpty_whenTextIsNotAnInt(final String text) {
        final OptionalInt number = NumberUtils.parseInt(text);

        assertThat(number).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9999999999", " 9999999999 "})
    void parseLong_shouldReturnNumber_whenTextIsAWholeNumber(final String text) {
        final OptionalLong number = NumberUtils.parseLong(text);

        assertThat(number).hasValue(9_999_999_999L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "-", "4x", "4.5", "+4", "٤٢", "99999999999999999999"})
    void parseLong_shouldReturnEmpty_whenTextIsNotALong(final String text) {
        final OptionalLong number = NumberUtils.parseLong(text);

        assertThat(number).isEmpty();
    }
}
