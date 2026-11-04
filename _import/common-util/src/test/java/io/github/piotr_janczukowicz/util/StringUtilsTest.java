package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringUtilsTest {

    @Test
    void constructor_shouldThrow_whenInstantiatedViaReflection() throws ReflectiveOperationException {
        UtilityClassAssertions.assertNotInstantiable(StringUtils.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n\r", "\u000B\f", "\u2003", "\u2003\u2003"})
    void isBlank_shouldReturnTrue_whenNullOrOnlyWhitespace(final String value) {
        final boolean isBlank = StringUtils.isBlank(value);

        assertThat(isBlank).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u00A0", "\u2007", "\u202F", "\u00A0 ", " a ", "\u2003a\u2003", "a"})
    void isBlank_shouldReturnFalse_whenContainsNonWhitespace(final String value) {
        final boolean isBlank = StringUtils.isBlank(value);

        assertThat(isBlank).isFalse();
    }

    @ParameterizedTest
    @MethodSource("blankBuilders")
    void isBlank_shouldReturnTrue_whenNonStringCharSequenceIsOnlyWhitespace(final CharSequence value) {
        final boolean isBlank = StringUtils.isBlank(value);

        assertThat(isBlank).isTrue();
    }

    @ParameterizedTest
    @MethodSource("notBlankBuilders")
    void isBlank_shouldReturnFalse_whenNonStringCharSequenceHasNonWhitespace(final CharSequence value) {
        final boolean isBlank = StringUtils.isBlank(value);

        assertThat(isBlank).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n\r", "\u000B\f", "\u2003", "\u2003\u2003"})
    void isNotBlank_shouldReturnFalse_whenNullOrOnlyWhitespace(final String value) {
        final boolean isNotBlank = StringUtils.isNotBlank(value);

        assertThat(isNotBlank).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u00A0", "\u2007", "\u202F", "\u00A0 ", " a ", "\u2003a\u2003", "a"})
    void isNotBlank_shouldReturnTrue_whenContainsNonWhitespace(final String value) {
        final boolean isNotBlank = StringUtils.isNotBlank(value);

        assertThat(isNotBlank).isTrue();
    }

    @ParameterizedTest
    @MethodSource("blankBuilders")
    void isNotBlank_shouldReturnFalse_whenNonStringCharSequenceIsOnlyWhitespace(final CharSequence value) {
        final boolean isNotBlank = StringUtils.isNotBlank(value);

        assertThat(isNotBlank).isFalse();
    }

    @ParameterizedTest
    @MethodSource("notBlankBuilders")
    void isNotBlank_shouldReturnTrue_whenNonStringCharSequenceHasNonWhitespace(final CharSequence value) {
        final boolean isNotBlank = StringUtils.isNotBlank(value);

        assertThat(isNotBlank).isTrue();
    }

    @ParameterizedTest
    @MethodSource("stripToNullCases")
    void stripToNull_shouldStripWhitespaceOrReturnNull_whenGivenVariousInputs(
            final String value, final String expected) {
        final String stripped = StringUtils.stripToNull(value);

        assertThat(stripped).isEqualTo(expected);
    }

    static Stream<CharSequence> blankBuilders() {
        return Stream.of(
                new StringBuilder(),
                new StringBuilder(" "),
                new StringBuilder("\t\n\r"),
                new StringBuilder("\u2003"),
                new StringBuilder(" \u2003 "));
    }

    static Stream<CharSequence> notBlankBuilders() {
        return Stream.of(
                new StringBuilder("a"),
                new StringBuilder(" a "),
                new StringBuilder("\u2003a\u2003"),
                new StringBuilder("\u00A0"),
                new StringBuilder("ὠ0"));
    }

    static Stream<Arguments> stripToNullCases() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of(" ", null),
                Arguments.of("\t\n\r", null),
                Arguments.of("\u2003", null),
                Arguments.of("\u00A0", "\u00A0"),
                Arguments.of(" a ", "a"),
                Arguments.of("\u2003a\u2003", "a"),
                Arguments.of("a", "a"),
                Arguments.of("\u00A0x\u00A0", "\u00A0x\u00A0"));
    }
}
