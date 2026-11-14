package io.github.piotr_janczukowicz.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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

    @Test
    void replaceControlCharacters_shouldReplaceEachControlCharacter_whenTextHoldsThem() {
        final String text = "a\nb\r\tc\u001b[0m\u0085d\u007f";

        final String cleaned = StringUtils.replaceControlCharacters(text, ' ');

        assertThat(cleaned).isEqualTo("a b  c [0m d ");
    }

    @Test
    void replaceControlCharacters_shouldKeepTheSameInstance_whenTextHoldsNoControlCharacter() {
        final String text = "plain   text   😀";

        final String cleaned = StringUtils.replaceControlCharacters(text, '?');

        assertThat(cleaned).isSameAs(text);
    }

    @Test
    void replaceControlCharacters_shouldUseTheGivenReplacement_whenTextHoldsAControlCharacter() {
        final String cleaned = StringUtils.replaceControlCharacters("a\nb", '_');

        assertThat(cleaned).isEqualTo("a_b");
    }

    @Test
    void replaceControlCharacters_shouldThrowNullPointerException_whenTextIsNull() {
        final Throwable thrown = catchThrowable(() -> StringUtils.replaceControlCharacters(null, ' '));

        assertThat(thrown).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
    }

    @Test
    void limited_shouldKeepTheSameInstance_whenTextFits() {
        final String text = "abc";

        final String limited = StringUtils.limited(text, 3);

        assertThat(limited).isSameAs(text);
    }

    @Test
    void limited_shouldCutTheText_whenItIsLonger() {
        final String limited = StringUtils.limited("abcdef", 3);

        assertThat(limited).isEqualTo("abc");
    }

    @Test
    void limited_shouldDropTheWholePair_whenTheCutIsInsideASurrogatePair() {
        final String limited = StringUtils.limited("ab\uD83D\uDE00cd", 3);

        assertThat(limited).isEqualTo("ab");
    }

    @Test
    void limited_shouldKeepThePair_whenTheCutIsAfterIt() {
        final String limited = StringUtils.limited("ab\uD83D\uDE00cd", 4);

        assertThat(limited).isEqualTo("ab\uD83D\uDE00");
    }

    @Test
    void limited_shouldThrow_whenTheTextIsNullOrTheLimitIsNotPositive() {
        final Throwable noText = catchThrowable(() -> StringUtils.limited(null, 3));
        final Throwable noLimit = catchThrowable(() -> StringUtils.limited("abc", 0));

        assertThat(noText).isInstanceOf(NullPointerException.class).hasMessageContaining("text");
        assertThat(noLimit).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("maxChars");
    }
}
