package io.github.piotr_janczukowicz.testsupport.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SpecExamplesTest {

    private static List<DynamicTest> mixed() {
        return SpecExamples.tests(
                        SpecExamplesTest.class,
                        "spec-examples-mixed.json",
                        SampleSupport.Sample.class,
                        SampleSupport.codec())
                .toList();
    }

    @Test
    void tests_shouldBeNamedByTheirSource_whenTheResourceListsExamples() {
        assertThat(mixed())
                .extracting(DynamicTest::getDisplayName)
                .containsExactly("decodes", "refused and throws", "refused but decodes", "decodes not");
    }

    @Test
    void tests_shouldPass_whenAnExampleDecodesOrARefusedOneThrows() {
        assertThat(catchThrowable(() -> mixed().get(0).getExecutable().execute()))
                .isNull();
        assertThat(catchThrowable(() -> mixed().get(1).getExecutable().execute()))
                .isNull();
    }

    @Test
    void tests_shouldFail_whenARefusedExampleDecodes() {
        assertThat(catchThrowable(() -> mixed().get(2).getExecutable().execute()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("refused but decodes");
    }

    @Test
    void tests_shouldFail_whenAnExampleDoesNotDecode() {
        assertThat(catchThrowable(() -> mixed().get(3).getExecutable().execute()))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("decodes not");
    }

    @Test
    void tests_shouldBeEmpty_whenTheTypeHasNoExamples() {
        assertThat(SpecExamples.tests(SpecExamplesTest.class, null, SampleSupport.Sample.class, SampleSupport.codec()))
                .isEmpty();
    }
}
