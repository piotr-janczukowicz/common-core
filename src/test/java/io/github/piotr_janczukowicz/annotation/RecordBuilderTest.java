package io.github.piotr_janczukowicz.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecordBuilderTest {

    @Test
    void build_shouldEqualCanonicalConstructor_whenAllFieldsSet() {
        final String name = "octocat";
        final int count = 42;
        final SampleRecord expected = new SampleRecord(name, count);

        final SampleRecord built =
                SampleRecordBuilder.builder().name(name).count(count).build();

        assertThat(built).isEqualTo(expected);
    }
}
